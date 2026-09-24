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
import ar.edu.utn.donatrack.donaciones.DonacionId;
import ar.edu.utn.donatrack.donaciones.DonacionesRepository;
import ar.edu.utn.donatrack.donaciones.EstadoDonacion;
import ar.edu.utn.donatrack.donaciones.PropuestaRepository;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.DonantesRepository;
import ar.edu.utn.donatrack.notificaciones.Notificacion;
import ar.edu.utn.donatrack.notificaciones.ServicioNotificaciones;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.http.Context;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DonacionController implements WithSimplePersistenceUnit {
  private final DonacionesRepository donaciones;
  private final DonantesRepository donantes;
  private final PropuestaRepository propuestas;
  private final CargaDonacion cargaDonacion;
  private final ServicioNotificaciones notificaciones;
  private final SubcategoriasRepository subcategorias;

  public DonacionController(
      DonacionesRepository donaciones,
      DonantesRepository donantes,
      PropuestaRepository propuestas,
      CargaDonacion cargaDonacion,
      ServicioNotificaciones notificaciones,
      SubcategoriasRepository subcategorias) {
    this.donaciones = donaciones;
    this.donantes = donantes;
    this.propuestas = propuestas;
    this.cargaDonacion = cargaDonacion;
    this.notificaciones = notificaciones;
    this.subcategorias = subcategorias;
  }

  public void routes() {
    path(
        "/donaciones",
        () -> {
          get(this::listar);
          post(this::crear);
          patch("/estado", this::transicionarMasivo);
          path(
              "/{id}",
              () -> {
                get(this::obtener);
                put(this::actualizar);
                delete(this::eliminar);
                patch("/estado", this::transicionar);
                get("/historial-estados", this::historial);
                post("/fotos", this::agregarFoto);
              });          
        });
  }

  private void listar(Context ctx) {
    ctx.json(donaciones.getDonaciones().stream().map(ApiMapper::donacion).toList());
  }

  private void obtener(Context ctx) {
    ctx.json(donacion(buscar(HttpSupport.id(ctx, "id"))));
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
    propuestas.remove(donacion);
    DonacionResponse actualizada =
        withTransaction(
            () -> {
              donacion.reemplazarBienes(bienes(request.bienes(), new HashMap<>()));
              return donacion(donaciones.save(donacion));
            });
    ctx.json(actualizada);
  }

  private void eliminar(Context ctx) {
    Donacion donacion = buscar(HttpSupport.id(ctx, "id"));
    if (donacion.getEstado() != EstadoDonacion.EN_DEPOSITO) {
      throw ApiException.conflict("Sólo se puede eliminar una donación en depósito.");
    }
    propuestas.remove(donacion);
    withTransaction(() -> donaciones.deleteById(donacion.getId()));
    ctx.status(204);
  }

  private void transicionar(Context ctx) {
    TransicionEstadoRequest request =
        DtoValidator.validate(ctx.bodyAsClass(TransicionEstadoRequest.class));
    Donacion donacion = buscar(HttpSupport.id(ctx, "id"));
    List<Notificacion> generadas = new ArrayList<>();

    withTransaction(() -> {
      generadas.addAll(cambiarEstado(donacion, request));
      donaciones.save(donacion);
    });

    notificaciones.enviar(generadas);
    ctx.json(donacion(donacion));
  }

  private void historial(Context ctx) {
    ctx.json(
        buscar(HttpSupport.id(ctx, "id")).getHistorialEstado().stream()
            .map(ApiMapper::historial)
            .toList());
  }

  private void agregarFoto(Context ctx) {
    FotoRequest request = DtoValidator.validate(ctx.bodyAsClass(FotoRequest.class));
    var id = HttpSupport.id(ctx, "id");
    Donacion donacion = buscar(id);

    withTransaction(() -> {
      donacion.agregarFoto(request.url());
      donaciones.save(donacion);
    });

    ctx.status(201).json(new FotoResponse(id, request.url()));
  }

  private Donacion buscar(UUID id) {
    return donaciones
        .findById(new DonacionId(id))
        .orElseThrow(() -> ApiException.notFound("Donación", id));
  }

  private List<Bien> bienes(List<BienRequest> items, Map<String, SubCategoria> cache) {
    return items.stream()
        .map(
            b ->
                new Bien(
                    b.descripcion(),
                    b.foto(),
                    b.cantidad(),
                    b.unidad(),
                    b.fechaVencimiento(),
                    b.estadoDeUso() == null ? EstadoUso.NO_APLICA : b.estadoDeUso(),
                    subcategorias.obtenerOCrear(b.subcategoria(), b.categoria(), cache)))
        .toList();
  }

  private List<Notificacion> cambiarEstado(Donacion donacion, TransicionEstadoRequest request) {
    return switch (request.estado()) {
      case LISTA_PARA_ENTREGAR -> donacion.planificarRuta();
      case EN_TRASLADO -> donacion.iniciarTraslado(request.urlMapa());
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
    String idCamion
  ) {}

  private void transicionarMasivo(Context ctx) {
    TransicionMasivaRequest[] requests = ctx.bodyAsClass(TransicionMasivaRequest[].class);

    for (TransicionMasivaRequest req : requests) {
      DtoValidator.validate(req);
      buscar(req.idDonacion());
    }

    List<Notificacion> generadas = new ArrayList<>();
    withTransaction(() -> {
      for (TransicionMasivaRequest req : requests) {
        Donacion donacion = buscar(req.idDonacion());
        String urlGenerada = req.idCamion() != null ? "https://mapa.donatrack.org/tracking/" + req.idCamion() : null;
        generadas.addAll(cambiarEstado(donacion, new TransicionEstadoRequest(req.nuevoEstado(), req.justificacion(), urlGenerada)));
        donaciones.save(donacion);
      }
    });

    notificaciones.enviar(generadas);
    ctx.status(200);
  }
}
