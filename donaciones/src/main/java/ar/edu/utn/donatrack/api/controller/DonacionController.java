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
import ar.edu.utn.donatrack.donaciones.CargaDonacion;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.DonacionId;
import ar.edu.utn.donatrack.donaciones.DonacionesRepository;
import ar.edu.utn.donatrack.donaciones.EstadoDonacion;
import ar.edu.utn.donatrack.donaciones.PropuestaRepository;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.DonantesRepository;
import io.javalin.http.Context;
import java.util.List;
import java.util.UUID;

public final class DonacionController {
  private final DonacionesRepository donaciones;
  private final DonantesRepository donantes;
  private final PropuestaRepository propuestas;
  private final CargaDonacion cargaDonacion;

  public DonacionController(
      DonacionesRepository donaciones,
      DonantesRepository donantes,
      PropuestaRepository propuestas,
      CargaDonacion cargaDonacion) {
    this.donaciones = donaciones;
    this.donantes = donantes;
    this.propuestas = propuestas;
    this.cargaDonacion = cargaDonacion;
  }

  public void routes() {
    path(
        "/donaciones",
        () -> {
          get(this::listar);
          post(this::crear);
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
    ctx.status(201)
        .json(
            cargaDonacion.cargarDonacion(donante, bienes(request.bienes())).stream()
                .map(ApiMapper::donacion)
                .toList());
  }

  private void actualizar(Context ctx) {
    Donacion donacion = buscar(HttpSupport.id(ctx, "id"));
    if (donacion.getEstado() != EstadoDonacion.EN_DEPOSITO)
      throw ApiException.conflict("Sólo se puede modificar una donación en depósito.");
    DonacionUpdateRequest request =
        DtoValidator.validate(ctx.bodyAsClass(DonacionUpdateRequest.class));
    donacion.reemplazarBienes(bienes(request.bienes()));
    propuestas.remove(donacion);
    ctx.json(donacion(donaciones.save(donacion)));
  }

  private void eliminar(Context ctx) {
    Donacion donacion = buscar(HttpSupport.id(ctx, "id"));
    if (donacion.getEstado() != EstadoDonacion.EN_DEPOSITO)
      throw ApiException.conflict("Sólo se puede eliminar una donación en depósito.");
    propuestas.remove(donacion);
    donaciones.deleteById(donacion.getId());
    ctx.status(204);
  }

  private void transicionar(Context ctx) {
    TransicionEstadoRequest request =
        DtoValidator.validate(ctx.bodyAsClass(TransicionEstadoRequest.class));
    Donacion donacion = buscar(HttpSupport.id(ctx, "id"));
    ctx.json(donacion(cambiarEstado(donacion, request)));
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
    buscar(id).agregarFoto(request.url());
    ctx.status(201).json(new FotoResponse(id, request.url()));
  }

  private Donacion buscar(UUID id) {
    return donaciones
        .findById(new DonacionId(id))
        .orElseThrow(() -> ApiException.notFound("Donación", id));
  }

  private List<Bien> bienes(List<BienRequest> items) {
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
                    new SubCategoria(b.subcategoria(), b.categoria())))
        .toList();
  }

  private Donacion cambiarEstado(Donacion donacion, TransicionEstadoRequest request) {
    return switch (request.estado()) {
      case LISTA_PARA_ENTREGAR -> {
        donacion.planificarRuta();
        yield donacion;
      }
      case EN_TRASLADO -> {
        donacion.iniciarTraslado();
        yield donacion;
      }
      case ENTREGADA -> {
        donacion.confirmarEntrega();
        yield donacion;
      }
      case ENTREGA_FALLIDA -> {
        donacion.reportarFalla(request.justificacion());
        yield donacion;
      }
      case VENCIDA -> {
        donacion.marcarVencida();
        propuestas.remove(donacion);
        yield donacion;
      }
      case EN_DEPOSITO -> {
        donacion.retornarAlDeposito();
        yield donacion;
      }
      case ASIGNACION_REALIZADA -> throw new IllegalArgumentException("Estado no permitido.");
    };
  }
}
