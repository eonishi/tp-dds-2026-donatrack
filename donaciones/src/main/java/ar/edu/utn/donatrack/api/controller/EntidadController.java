package ar.edu.utn.donatrack.api.controller;

import static ar.edu.utn.donatrack.api.mappers.ApiMapper.entidad;
import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.put;

import ar.edu.utn.donatrack.api.dto.EntidadDtos.EntidadRequest;
import ar.edu.utn.donatrack.api.dto.EntidadDtos.EntidadResponse;
import ar.edu.utn.donatrack.api.error.ApiException;
import ar.edu.utn.donatrack.api.validation.DtoValidator;
import ar.edu.utn.donatrack.beneficiarios.BeneficiariosRepository;
import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donaciones.DonacionesRepository;
import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.http.Context;
import java.util.List;

public final class EntidadController implements WithSimplePersistenceUnit {
  private final BeneficiariosRepository entidades;
  private final DonacionesRepository donaciones;

  public EntidadController(BeneficiariosRepository entidades, DonacionesRepository donaciones) {
    this.entidades = entidades;
    this.donaciones = donaciones;
  }

  public void routes() {
    path(
        "/beneficiarias",
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
    ctx.json(entidades.getBeneficiarios().stream().map(this::response).toList());
  }

  private void obtener(Context ctx) {
    ctx.json(response(buscar(entidadId(ctx, "id"))));
  }

  private void crear(Context ctx) {
    EntidadRequest request = DtoValidator.validate(ctx.bodyAsClass(EntidadRequest.class));
    validarRazonSocial(request.razonSocial(), null);
    EntidadBeneficiaria nueva =
        new EntidadBeneficiaria(request.razonSocial(), request.direccion(), request.telefono());
    nueva.actualizarDatos(
        request.razonSocial(), request.direccion(), request.telefono(), correos(request));
    EntidadResponse creada = withTransaction(() -> response(entidades.save(nueva)));
    ctx.status(201).json(creada);
  }

  private void actualizar(Context ctx) {
    Long id = entidadId(ctx, "id");
    EntidadBeneficiaria entidad = buscar(id);
    EntidadRequest request = DtoValidator.validate(ctx.bodyAsClass(EntidadRequest.class));
    validarRazonSocial(request.razonSocial(), id);
    entidad.actualizarDatos(
        request.razonSocial(), request.direccion(), request.telefono(), correos(request));
    EntidadResponse actualizada = withTransaction(() -> response(entidades.save(entidad)));
    ctx.json(actualizada);
  }

  private void eliminar(Context ctx) {
    Long id = entidadId(ctx, "id");
    buscar(id);
    if (donaciones.getDonaciones().stream()
        .anyMatch(
            d ->
                d.getEntidadBeneficiaria() != null
                    && d.getEntidadBeneficiaria().getId().equals(id))) {
      throw ApiException.conflict("No se puede eliminar una entidad con donaciones asignadas.");
    }
    withTransaction(() -> entidades.deleteById(id));
    ctx.status(204);
  }

  private EntidadResponse response(EntidadBeneficiaria entidad) {
    return entidad(entidad);
  }

  private EntidadBeneficiaria buscar(Long id) {
    return entidades
        .findById(id)
        .orElseThrow(() -> ApiException.notFound("Entidad beneficiaria", id));
  }

  private Long entidadId(Context ctx, String parameter) {
    return HttpSupport.longId(ctx, parameter);
  }

  private void validarRazonSocial(String razonSocial, Long actual) {
    if (entidades.getBeneficiarios().stream()
        .anyMatch(
            e -> !e.getId().equals(actual) && e.getRazonSocial().equalsIgnoreCase(razonSocial))) {
      throw ApiException.conflict("Ya existe una entidad con esa razón social.");
    }
  }

  private List<CorreoElectronico> correos(EntidadRequest request) {
    return request.correos() == null
        ? List.of()
        : request.correos().stream().map(CorreoElectronico::new).toList();
  }
}
