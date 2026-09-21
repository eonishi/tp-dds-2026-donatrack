package ar.edu.utn.donatrack.api.controller;

import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.put;

import ar.edu.utn.donatrack.api.dto.NecesidadDtos.NecesidadRequest;
import ar.edu.utn.donatrack.api.dto.NecesidadDtos.NecesidadResponse;
import ar.edu.utn.donatrack.api.error.ApiException;
import ar.edu.utn.donatrack.api.mappers.ApiMapper;
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
        "/entidades/{entidadId}/necesidades",
        () -> {
          get(this::listar);
          post(this::crear);
          path(
              "/{necesidadId}",
              () -> {
                get(this::obtener);
                put(this::actualizar);
                delete(this::eliminar);
              });
        });
  }

  private void listar(Context ctx) {
    EntidadBeneficiaria entidad = entidad(HttpSupport.id(ctx, "entidadId"));
    ctx.json(
        entidad.necesidades().stream().map(necesidad -> response(necesidad, entidad)).toList());
  }

  private void obtener(Context ctx) {
    EntidadBeneficiaria entidad = entidad(HttpSupport.id(ctx, "entidadId"));
    ctx.json(response(necesidad(entidad, HttpSupport.id(ctx, "necesidadId")), entidad));
  }

  private void crear(Context ctx) {
    NecesidadRequest request = DtoValidator.validate(ctx.bodyAsClass(NecesidadRequest.class));
    validar(request);
    EntidadBeneficiaria entidad = entidad(HttpSupport.id(ctx, "entidadId"));
    Necesidad necesidad = construirNecesidad(request);
    agregar(entidad, necesidad);
    ctx.status(201).json(ApiMapper.necesidad(necesidad, entidad));
  }

  private void actualizar(Context ctx) {
    EntidadBeneficiaria entidad = entidad(HttpSupport.id(ctx, "entidadId"));
    Necesidad actual = necesidad(entidad, HttpSupport.id(ctx, "necesidadId"));
    NecesidadRequest request = DtoValidator.validate(ctx.bodyAsClass(NecesidadRequest.class));
    validar(request);
    boolean esRecurrente = actual instanceof NecesidadRecurrente;
    if (esRecurrente != recurrente(request)) {
      throw ApiException.conflict("No se puede cambiar el tipo de una necesidad.");
    }
    actual.actualizar(
        new SubCategoria(request.subcategoria(), request.categoria()),
        request.cantidadObjetivo(),
        request.unidad(),
        request.descripcion());
    if (esRecurrente) {
      ((NecesidadRecurrente) actual).setPeriodo(request.periodo());
    }
    ctx.json(ApiMapper.necesidad(actual, entidad));
  }

  private void eliminar(Context ctx) {
    EntidadBeneficiaria entidad = entidad(HttpSupport.id(ctx, "entidadId"));
    Necesidad necesidad = necesidad(entidad, HttpSupport.id(ctx, "necesidadId"));
    entidad.eliminarNecesidad(necesidad.getId());
    ctx.status(204);
  }

  private NecesidadResponse response(Necesidad necesidad, EntidadBeneficiaria entidad) {
    return ApiMapper.necesidad(necesidad, entidad);
  }

  private Necesidad necesidad(EntidadBeneficiaria entidad, UUID necesidadId) {
    return entidad
        .buscarNecesidad(necesidadId)
        .orElseThrow(() -> ApiException.notFound("Necesidad", necesidadId));
  }

  private EntidadBeneficiaria entidad(UUID id) {
    return entidades
        .findById(id)
        .orElseThrow(() -> ApiException.notFound("Entidad beneficiaria", id));
  }

  private Necesidad construirNecesidad(NecesidadRequest request) {
    SubCategoria subcategoria = new SubCategoria(request.subcategoria(), request.categoria());
    if (!recurrente(request)) {
      return new Necesidad(
          subcategoria, request.cantidadObjetivo(), request.unidad(), request.descripcion());
    }
    NecesidadRecurrente necesidad =
        new NecesidadRecurrente(
            subcategoria, request.cantidadObjetivo(), request.unidad(), request.descripcion());
    necesidad.setPeriodo(request.periodo());
    return necesidad;
  }

  private void agregar(EntidadBeneficiaria entidad, Necesidad necesidad) {
    if (necesidad instanceof NecesidadRecurrente recurrente)
      entidad.agregarNecesidadRecurrente(recurrente);
    else entidad.agregarNecesidadExtraordinaria(necesidad);
  }

  private void validar(NecesidadRequest request) {
    if (!request.tipo().equalsIgnoreCase("EXTRAORDINARIA")
        && !request.tipo().equalsIgnoreCase("RECURRENTE")) {
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
