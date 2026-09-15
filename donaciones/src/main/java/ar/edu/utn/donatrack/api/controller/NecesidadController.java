package ar.edu.utn.donatrack.api.controller;

import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.put;

import ar.edu.utn.donatrack.api.mappers.ApiMapper;
import ar.edu.utn.donatrack.api.dto.NecesidadDtos.NecesidadRequest;
import ar.edu.utn.donatrack.api.error.ApiException;
import ar.edu.utn.donatrack.api.validation.DtoValidator;
import ar.edu.utn.donatrack.beneficiarios.BeneficiariosRepository;
import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.beneficiarios.Necesidad;
import ar.edu.utn.donatrack.beneficiarios.NecesidadRecurrente;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import io.javalin.http.Context;
import java.util.Locale;
import java.util.UUID;

public final class NecesidadController {
  private final BeneficiariosRepository entidades;

  public NecesidadController(BeneficiariosRepository entidades) {
    this.entidades = entidades;
  }

  public void routes() {
    path(
        "/api/v1/necesidades",
        () -> {
          get(this::listar);
          post(this::crear);
          path(
              "/{id}",
              () -> {
                get(this::obtener);
                put(this::actualizar);
                delete(this::eliminar);
              });
        });
  }

  private void listar(Context ctx) {
    ctx.json(todas().stream().map(this::response).toList());
  }

  private void obtener(Context ctx) {
    ctx.json(response(buscar(HttpSupport.id(ctx, "id"))));
  }

  private void crear(Context ctx) {
    NecesidadRequest request = DtoValidator.validate(ctx.bodyAsClass(NecesidadRequest.class));
    validar(request);
    EntidadBeneficiaria entidad = entidad(request.entidadBeneficiariaId());
    Necesidad necesidad = crear(request);
    agregar(entidad, necesidad);
    ctx.status(201).json(ApiMapper.necesidad(necesidad, entidad));
  }

  private void actualizar(Context ctx) {
    Ubicada actual = buscar(HttpSupport.id(ctx, "id"));
    NecesidadRequest request = DtoValidator.validate(ctx.bodyAsClass(NecesidadRequest.class));
    validar(request);
    boolean esRecurrente = actual.necesidad() instanceof NecesidadRecurrente;
    if (esRecurrente != recurrente(request)) {
      throw ApiException.conflict("No se puede cambiar el tipo de una necesidad.");
    }
    actual.necesidad().actualizar(new SubCategoria(request.subcategoria(), request.categoria()), request.cantidadObjetivo(), request.unidad(), request.descripcion());
    if (esRecurrente) {
      ((NecesidadRecurrente) actual.necesidad()).setPeriodo(request.periodo());
    }
    EntidadBeneficiaria destino = entidad(request.entidadBeneficiariaId());
    if (!actual.entidad().getId().equals(destino.getId())) {
      actual.entidad().eliminarNecesidad(actual.necesidad().getId());
      agregar(destino, actual.necesidad());
    }
    ctx.json(ApiMapper.necesidad(actual.necesidad(), destino));
  }

  private void eliminar(Context ctx) {
    Ubicada ubicada = buscar(HttpSupport.id(ctx, "id"));
    ubicada.entidad().eliminarNecesidad(ubicada.necesidad().getId());
    ctx.status(204);
  }

  private ar.edu.utn.donatrack.api.dto.NecesidadDtos.NecesidadResponse response(Ubicada ubicada) {
    return ApiMapper.necesidad(ubicada.necesidad(), ubicada.entidad());
  }

  private record Ubicada(Necesidad necesidad, EntidadBeneficiaria entidad) {}

  private java.util.List<Ubicada> todas() {
    return entidades.getBeneficiarios().stream().flatMap(e -> e.necesidades().stream().map(n -> new Ubicada(n, e))).toList();
  }

  private Ubicada buscar(UUID id) {
    return todas().stream().filter(n -> n.necesidad().getId().equals(id)).findFirst().orElseThrow(() -> ApiException.notFound("Necesidad", id));
  }

  private EntidadBeneficiaria entidad(UUID id) {
    return entidades.findById(id).orElseThrow(() -> ApiException.notFound("Entidad beneficiaria", id));
  }

  private Necesidad crear(NecesidadRequest request) {
    SubCategoria subcategoria = new SubCategoria(request.subcategoria(), request.categoria());
    if (!recurrente(request)) {
      return new Necesidad(subcategoria, request.cantidadObjetivo(), request.unidad(), request.descripcion());
    }
    NecesidadRecurrente necesidad = new NecesidadRecurrente(subcategoria, request.cantidadObjetivo(), request.unidad(), request.descripcion());
    necesidad.setPeriodo(request.periodo());
    return necesidad;
  }

  private void agregar(EntidadBeneficiaria entidad, Necesidad necesidad) {
    if (necesidad instanceof NecesidadRecurrente recurrente) entidad.agregarNecesidadRecurrente(recurrente);
    else entidad.agregarNecesidadExtraordinaria(necesidad);
  }

  private void validar(NecesidadRequest request) {
    if (!request.tipo().equalsIgnoreCase("EXTRAORDINARIA") && !request.tipo().equalsIgnoreCase("RECURRENTE")) {
      throw new IllegalArgumentException("tipo debe ser EXTRAORDINARIA o RECURRENTE.");
    }
    if (recurrente(request) && request.periodo() == null) {
      throw new IllegalArgumentException("periodo es obligatorio para una necesidad recurrente.");
    }
  }

  private boolean recurrente(NecesidadRequest request) {
    return request.tipo().toUpperCase(Locale.ROOT).equals("RECURRENTE");
  }
}
