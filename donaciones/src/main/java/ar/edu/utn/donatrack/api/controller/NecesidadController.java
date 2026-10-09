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
import ar.edu.utn.donatrack.bienes.SubcategoriasRepository;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.http.Context;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class NecesidadController implements WithSimplePersistenceUnit {
  private final BeneficiariosRepository entidades;
  private final SubcategoriasRepository subcategorias;

  public NecesidadController(
      BeneficiariosRepository entidades, SubcategoriasRepository subcategorias) {
    this.entidades = entidades;
    this.subcategorias = subcategorias;
  }

  public void routes() {
    path(
        "/beneficiarias/{entidadId}/necesidades",
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
    EntidadBeneficiaria entidad = entidad(ctx);
    ctx.json(
        entidad.necesidades().stream().map(necesidad -> response(necesidad, entidad)).toList());
  }

  private void obtener(Context ctx) {
    EntidadBeneficiaria entidad = entidad(ctx);
    ctx.json(response(necesidad(entidad, HttpSupport.id(ctx, "necesidadId")), entidad));
  }

  private void crear(Context ctx) {
    NecesidadRequest request = DtoValidator.validate(ctx.bodyAsClass(NecesidadRequest.class));
    validar(request);
    EntidadBeneficiaria entidad = entidad(ctx);
    Necesidad nuevaNecesidad =
        withTransaction(
            () -> {
              Necesidad necesidad = construirNecesidad(request, new HashMap<>());
              agregar(entidad, necesidad);
              entidades.save(entidad);
              return necesidad;
            });
    ctx.status(201).json(ApiMapper.necesidad(nuevaNecesidad, entidad));
  }

  private void actualizar(Context ctx) {
    EntidadBeneficiaria entidad = entidad(ctx);
    Necesidad actual = necesidad(entidad, HttpSupport.id(ctx, "necesidadId"));
    NecesidadRequest request = DtoValidator.validate(ctx.bodyAsClass(NecesidadRequest.class));
    validar(request);
    boolean esRecurrente = actual instanceof NecesidadRecurrente;
    if (esRecurrente != recurrente(request)) {
      throw ApiException.conflict("No se puede cambiar el tipo de una necesidad.");
    }
    NecesidadResponse actualizada =
        withTransaction(
            () -> {
              actual.actualizar(
                  subcategorias.obtenerOrCrear(
                      request.subcategoria(), request.categoria(), new HashMap<>()),
                  request.cantidadObjetivo(),
                  request.unidad(),
                  request.descripcion());
              if (esRecurrente) {
                ((NecesidadRecurrente) actual).setPeriodo(request.periodo());
              }
              entidades.save(entidad);
              return ApiMapper.necesidad(actual, entidad);
            });
    ctx.json(actualizada);
  }

  private void eliminar(Context ctx) {
    EntidadBeneficiaria entidad = entidad(ctx);
    Necesidad necesidad = necesidad(entidad, HttpSupport.id(ctx, "necesidadId"));
    withTransaction(() -> entidad.eliminarNecesidad(necesidad.getId()));
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

  private EntidadBeneficiaria entidad(Context ctx) {
    return entidad(HttpSupport.longId(ctx, "entidadId"));
  }

  private EntidadBeneficiaria entidad(Long id) {
    return entidades
        .findById(id)
        .orElseThrow(() -> ApiException.notFound("Entidad beneficiaria", id));
  }

  private Necesidad construirNecesidad(NecesidadRequest request, Map<String, SubCategoria> cache) {
    SubCategoria subcategoria =
        subcategorias.obtenerOrCrear(request.subcategoria(), request.categoria(), cache);
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
    if (necesidad instanceof NecesidadRecurrente recurrente) {
      entidad.agregarNecesidadRecurrente(recurrente);
    } else {
      entidad.agregarNecesidadExtraordinaria(necesidad);
    }
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
