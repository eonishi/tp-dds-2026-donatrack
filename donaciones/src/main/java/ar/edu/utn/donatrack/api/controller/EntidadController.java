package ar.edu.utn.donatrack.api.controller;

import static ar.edu.utn.donatrack.api.mappers.ApiMapper.entidad;
import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.put;

import ar.edu.utn.donatrack.api.dto.EntidadDtos.EntidadRequest;
import ar.edu.utn.donatrack.api.error.ApiException;
import ar.edu.utn.donatrack.api.mappers.ApiMapper;
import ar.edu.utn.donatrack.api.validation.DtoValidator;
import ar.edu.utn.donatrack.beneficiarios.BeneficiariosRepository;
import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donaciones.DonacionesRepository;
import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import io.javalin.http.Context;
import java.util.List;
import java.util.UUID;

public final class EntidadController {
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
    ctx.json(entidades.getBeneficiarios().stream().map(ApiMapper::entidad).toList());
  }

  private void obtener(Context ctx) {
    ctx.json(entidad(buscar(HttpSupport.id(ctx, "id"))));
  }

  private void crear(Context ctx) {
    EntidadRequest request = DtoValidator.validate(ctx.bodyAsClass(EntidadRequest.class));
    validarRazonSocial(request.razonSocial(), null);
    EntidadBeneficiaria nueva = new EntidadBeneficiaria(request.razonSocial(), request.direccion(), request.telefono());
    nueva.actualizarDatos(request.razonSocial(), request.direccion(), request.telefono(), correos(request));
    ctx.status(201).json(entidad(entidades.save(nueva)));
  }

  private void actualizar(Context ctx) {
    UUID id = HttpSupport.id(ctx, "id");
    EntidadBeneficiaria entidad = buscar(id);
    EntidadRequest request = DtoValidator.validate(ctx.bodyAsClass(EntidadRequest.class));
    validarRazonSocial(request.razonSocial(), id);
    entidad.actualizarDatos(request.razonSocial(), request.direccion(), request.telefono(), correos(request));
    ctx.json(entidad(entidades.save(entidad)));
  }

  private void eliminar(Context ctx) {
    UUID id = HttpSupport.id(ctx, "id");
    buscar(id);
    if (donaciones.getDonaciones().stream().anyMatch(d -> d.getEntidadBeneficiaria() != null && d.getEntidadBeneficiaria().getId().equals(id))) {
      throw ApiException.conflict("No se puede eliminar una entidad con donaciones asignadas.");
    }
    entidades.deleteById(id);
    ctx.status(204);
  }

  private EntidadBeneficiaria buscar(UUID id) {
    return entidades.findById(id).orElseThrow(() -> ApiException.notFound("Entidad beneficiaria", id));
  }

  private void validarRazonSocial(String razonSocial, UUID actual) {
    if (entidades.getBeneficiarios().stream().anyMatch(e -> !e.getId().equals(actual) && e.getRazonSocial().equalsIgnoreCase(razonSocial))) {
      throw ApiException.conflict("Ya existe una entidad con esa razón social.");
    }
  }

  private List<CorreoElectronico> correos(EntidadRequest request) {
    return request.correos() == null ? List.of() : request.correos().stream().map(CorreoElectronico::new).toList();
  }
}
