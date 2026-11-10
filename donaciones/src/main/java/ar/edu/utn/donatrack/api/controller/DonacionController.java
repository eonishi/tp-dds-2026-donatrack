package ar.edu.utn.donatrack.api.controller;

import static ar.edu.utn.donatrack.api.mappers.ApiMapper.donacion;
import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.patch;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.put;

import ar.edu.utn.donatrack.api.dto.DonacionDtos.BienRequest;
import ar.edu.utn.donatrack.api.dto.DonacionDtos.CargaDonacionRequest;
import ar.edu.utn.donatrack.api.dto.DonacionDtos.DonacionResponse;
import ar.edu.utn.donatrack.api.dto.DonacionDtos.DonacionUpdateRequest;
import ar.edu.utn.donatrack.api.dto.DonacionDtos.FotoRequest;
import ar.edu.utn.donatrack.api.dto.DonacionDtos.FotoResponse;
import ar.edu.utn.donatrack.api.dto.DonacionDtos.TransicionEstadoRequest;
import ar.edu.utn.donatrack.api.error.ApiException;
import ar.edu.utn.donatrack.api.mappers.ApiMapper;
import ar.edu.utn.donatrack.api.validation.DtoValidator;
import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.bienes.EstadoUso;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.bienes.SubcategoriasRepository;
import ar.edu.utn.donatrack.donaciones.CargaDonacion;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.DonacionesRepository;
import ar.edu.utn.donatrack.donaciones.EstadoDonacion;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.DonantesRepository;
import ar.edu.utn.donatrack.notificaciones.Notificacion;
import ar.edu.utn.donatrack.notificaciones.ServicioNotificaciones;
import ar.edu.utn.donatrack.propuestas.PropuestaRepository;
import ar.edu.utn.donatrack.security.Role;
import ar.edu.utn.donatrack.security.ServicioInterno;
import ar.edu.utn.donatrack.seguimiento.LogisticaNoDisponibleException;
import ar.edu.utn.donatrack.seguimiento.PosicionCamion;
import ar.edu.utn.donatrack.seguimiento.PosicionesCamiones;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.http.Context;
import io.javalin.security.RouteRole;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DonacionController implements WithSimplePersistenceUnit {
  private final DonacionesRepository donaciones;
  private final DonantesRepository donantes;
  private final PropuestaRepository propuestas;
  private final CargaDonacion cargaDonacion;
  private final ServicioNotificaciones notificaciones;
  private final SubcategoriasRepository subcategorias;
  private final PosicionesCamiones posicionesCamiones;

  // Lo único que la entidad beneficiaria puede hacer con el estado: confirmar que la recibió o
  // informar que no llegó.
  private static final Set<EstadoDonacion> ESTADOS_DE_LA_BENEFICIARIA =
      Set.of(EstadoDonacion.ENTREGADA, EstadoDonacion.ENTREGA_FALLIDA);

  public DonacionController(
      DonacionesRepository donaciones,
      DonantesRepository donantes,
      PropuestaRepository propuestas,
      CargaDonacion cargaDonacion,
      ServicioNotificaciones notificaciones,
      SubcategoriasRepository subcategorias,
      PosicionesCamiones posicionesCamiones) {
    this.posicionesCamiones = posicionesCamiones;
    this.donaciones = donaciones;
    this.donantes = donantes;
    this.propuestas = propuestas;
    this.cargaDonacion = cargaDonacion;
    this.notificaciones = notificaciones;
    this.subcategorias = subcategorias;
  }

  // Logística entra a todas con la clave interna: consulta las donaciones a planificar y les
  // cambia el estado al iniciar una ruta.
  // Consultas: el handler filtra para que cada persona vea solo sus donaciones (AccesoDonaciones).
  private static final RouteRole[] CONSULTAN = {
    Role.ADMIN, Role.DONANTE, Role.BENEFICIARIO, ServicioInterno.LOGISTICA
  };
  // La entidad confirma la recepción (o informa que no llegó) y carga las fotos de lo que le
  // asignaron; el handler verifica que la donación sea suya.
  private static final RouteRole[] RECIBEN = {
    Role.ADMIN, Role.BENEFICIARIO, ServicioInterno.LOGISTICA
  };
  // Alta, edición y baja las hace la persona administradora en el depósito.
  private static final RouteRole[] CARGAN = {Role.ADMIN, ServicioInterno.LOGISTICA};

  public void routes() {
    path(
        "/donaciones",
        () -> {
          get(this::listar, CONSULTAN);
          post(this::crear, CARGAN);
          patch("/estado", this::transicionarMasivo, RECIBEN);
          path(
              "/{id}",
              () -> {
                get(this::obtener, CONSULTAN);
                put(this::actualizar, CARGAN);
                delete(this::eliminar, CARGAN);
                patch("/estado", this::transicionar, RECIBEN);
                get("/historial-estados", this::historial, CONSULTAN);
                get("/seguimiento", this::seguimiento, CONSULTAN);
                post("/fotos", this::agregarFoto, RECIBEN);
              });
        });
  }

  // GET /donaciones?estado=ASIGNACION_REALIZADA: logística filtra las que tiene que planificar.
  private void listar(Context ctx) {
    EstadoDonacion estado = estadoFiltro(ctx.queryParam("estado"));
    ctx.json(
        donaciones.getDonaciones().stream()
            .filter(d -> estado == null || d.getEstado() == estado)
            .filter(d -> AccesoDonaciones.puedeVer(ctx, d))
            .map(ApiMapper::donacion)
            .toList());
  }

  private static EstadoDonacion estadoFiltro(String estado) {
    if (estado == null || estado.isBlank()) {
      return null;
    }
    try {
      return EstadoDonacion.valueOf(estado.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw ApiException.badRequest("Estado inválido: " + estado);
    }
  }

  private void obtener(Context ctx) {
    ctx.json(donacion(buscarVisible(ctx)));
  }

  /**
   * Dónde está el camión que lleva la donación. Solo mientras está en traslado y solo para quien
   * puede verla: la donante que la hizo, la entidad que la recibe y la administradora.
   */
  private void seguimiento(Context ctx) {
    Donacion donacion = buscarVisible(ctx);
    if (donacion.getEstado() != EstadoDonacion.EN_TRASLADO) {
      throw ApiException.conflict("La donación no está en traslado.");
    }
    String patente =
        donacion
            .getIdCamion()
            .orElseThrow(() -> ApiException.conflict("La donación no tiene un camión registrado."));
    try {
      PosicionCamion posicion =
          posicionesCamiones
              .ultimaPosicion(patente)
              .orElseThrow(
                  () ->
                      new ApiException(
                          404,
                          "NOT_FOUND",
                          "El camión " + patente + " todavía no reportó posición."));
      ctx.json(posicion);
    } catch (LogisticaNoDisponibleException e) {
      throw new ApiException(502, "BAD_GATEWAY", e.getMessage());
    }
  }

  private void crear(Context ctx) {
    CargaDonacionRequest request =
        DtoValidator.validate(ctx.bodyAsClass(CargaDonacionRequest.class));
    Donante donante =
        donantes
            .findById(request.donanteId())
            .orElseThrow(() -> ApiException.notFound("Donante", request.donanteId()));
    List<DonacionResponse> creadas =
        withTransaction(
            () -> {
              Map<String, SubCategoria> cache = new HashMap<>();
              return cargaDonacion.cargarDonacion(donante, bienes(request.bienes(), cache)).stream()
                  .map(ApiMapper::donacion)
                  .toList();
            });
    ctx.status(201).json(creadas);
  }

  private void actualizar(Context ctx) {
    Donacion donacion = buscar(HttpSupport.id(ctx, "id"));
    if (donacion.getEstado() != EstadoDonacion.EN_DEPOSITO) {
      throw ApiException.conflict("Sólo se puede modificar una donación en depósito.");
    }
    DonacionUpdateRequest request =
        DtoValidator.validate(ctx.bodyAsClass(DonacionUpdateRequest.class));
    DonacionResponse actualizada =
        withTransaction(
            () -> {
              donacion.reemplazarBienes(bienes(request.bienes(), new HashMap<>()));
              propuestas.remove(donacion);
              return donacion(donaciones.save(donacion));
            });
    ctx.json(actualizada);
  }

  private void eliminar(Context ctx) {
    Donacion donacion = buscar(HttpSupport.id(ctx, "id"));
    if (donacion.getEstado() != EstadoDonacion.EN_DEPOSITO) {
      throw ApiException.conflict("Sólo se puede eliminar una donación en depósito.");
    }
    withTransaction(
        () -> {
          propuestas.remove(donacion);
          donaciones.deleteById(donacion.getId());
        });
    ctx.status(204);
  }

  private void transicionar(Context ctx) {
    TransicionEstadoRequest request =
        DtoValidator.validate(ctx.bodyAsClass(TransicionEstadoRequest.class));
    Donacion donacion = buscarVisible(ctx);
    if (!AccesoDonaciones.accesoCompleto(ctx)
        && (!AccesoDonaciones.puedeOperarComoBeneficiaria(ctx, donacion)
            || !ESTADOS_DE_LA_BENEFICIARIA.contains(request.estado()))) {
      throw new ApiException(
          403, "FORBIDDEN", "Sólo puede confirmar o rechazar la recepción de sus donaciones.");
    }
    List<Notificacion> generadas = new ArrayList<>();

    withTransaction(
        () -> {
          generadas.addAll(cambiarEstado(donacion, request));
          donaciones.save(donacion);
        });

    notificaciones.enviar(generadas);
    ctx.json(donacion(donacion));
  }

  private void historial(Context ctx) {
    ctx.json(buscarVisible(ctx).getHistorialEstado().stream().map(ApiMapper::historial).toList());
  }

  private void agregarFoto(Context ctx) {
    FotoRequest request = DtoValidator.validate(ctx.bodyAsClass(FotoRequest.class));
    var id = HttpSupport.id(ctx, "id");
    Donacion donacion = buscarVisible(ctx);
    if (!AccesoDonaciones.puedeOperarComoBeneficiaria(ctx, donacion)) {
      throw new ApiException(403, "FORBIDDEN", "Sólo puede cargar fotos de sus donaciones.");
    }

    withTransaction(
        () -> {
          donacion.agregarFoto(request.url());
          donaciones.save(donacion);
        });

    ctx.status(201).json(new FotoResponse(id, request.url()));
  }

  // Si no la puede ver, responde como si no existiera para no revelar ids de otros.
  private Donacion buscarVisible(Context ctx) {
    UUID id = HttpSupport.id(ctx, "id");
    Donacion donacion = buscar(id);
    if (!AccesoDonaciones.puedeVer(ctx, donacion)) {
      throw ApiException.notFound("Donación", id);
    }
    return donacion;
  }

  private Donacion buscar(UUID id) {
    return donaciones.findById(id).orElseThrow(() -> ApiException.notFound("Donación", id));
  }

  private List<Bien> bienes(List<BienRequest> items, Map<String, SubCategoria> cache) {
    return items.stream()
        .map(
            b -> {
              SubCategoria subcategoria =
                  subcategorias.obtenerOrCrear(b.subcategoria(), b.categoria(), cache);
              if ((b.categoria().requiereEstadoDeUso()
                      || subcategoria.getCategoria().requiereEstadoDeUso())
                  && (b.estadoDeUso() == null || b.estadoDeUso() == EstadoUso.NO_APLICA)) {
                throw ApiException.badRequest(
                    "Debe indicar NUEVO o USADO para mobiliario y vestimenta.");
              }
              return new Bien(
                  b.descripcion(),
                  b.foto(),
                  b.cantidad(),
                  b.unidad(),
                  b.fechaVencimiento(),
                  b.estadoDeUso() == null ? EstadoUso.NO_APLICA : b.estadoDeUso(),
                  subcategoria);
            })
        .toList();
  }

  private List<Notificacion> cambiarEstado(Donacion donacion, TransicionEstadoRequest request) {
    return switch (request.estado()) {
      case LISTA_PARA_ENTREGAR -> donacion.planificarRuta();
      case EN_TRASLADO -> iniciarTraslado(donacion, request.idCamion());
      case ENTREGADA -> donacion.confirmarEntrega();
      case ENTREGA_FALLIDA -> donacion.reportarFalla(request.justificacion());
      case VENCIDA -> {
        var notifs = donacion.marcarVencida();
        propuestas.remove(donacion);
        yield notifs;
      }
      case EN_DEPOSITO -> donacion.retornarAlDeposito();
      case ASIGNACION_REALIZADA -> throw ApiException.badRequest("Estado no permitido.");
    };
  }

  public record TransicionMasivaRequest(
      @NotNull UUID idDonacion,
      @NotNull EstadoDonacion nuevoEstado,
      String justificacion,
      String idCamion) {}

  private void transicionarMasivo(Context ctx) {
    // La usan logística (con la clave interna) y la administración; no es para las beneficiarias.
    if (!AccesoDonaciones.accesoCompleto(ctx)) {
      throw new ApiException(403, "FORBIDDEN", "No tiene permisos para esta operación.");
    }
    TransicionMasivaRequest[] requests = ctx.bodyAsClass(TransicionMasivaRequest[].class);

    for (TransicionMasivaRequest req : requests) {
      DtoValidator.validate(req);
      buscar(req.idDonacion());
      if (req.nuevoEstado() == EstadoDonacion.EN_TRASLADO) {
        validarIdCamion(req.idCamion());
      }
    }

    List<Notificacion> generadas = new ArrayList<>();
    withTransaction(
        () -> {
          for (TransicionMasivaRequest req : requests) {
            Donacion donacion = buscar(req.idDonacion());
            generadas.addAll(
                cambiarEstado(
                    donacion,
                    new TransicionEstadoRequest(
                        req.nuevoEstado(), req.justificacion(), req.idCamion())));
            donaciones.save(donacion);
          }
        });

    notificaciones.enviar(generadas);
    ctx.status(200);
  }

  private List<ar.edu.utn.donatrack.notificaciones.Notificacion> iniciarTraslado(
      Donacion donacion, String idCamion) {
    validarIdCamion(idCamion);
    donacion.registrarCamion(idCamion);
    String urlMapa = "https://mapa.donatrack.org/tracking/" + idCamion;
    return donacion.iniciarTraslado(urlMapa);
  }

  private void validarIdCamion(String idCamion) {
    if (idCamion == null || idCamion.isBlank()) {
      throw new IllegalArgumentException("idCamion es obligatorio para iniciar el traslado.");
    }
  }
}
