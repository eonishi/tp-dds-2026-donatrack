package ar.edu.utn.donatrack.api.controller;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.put;

import ar.edu.utn.donatrack.api.dto.DonacionDtos.DonacionResponse;
import ar.edu.utn.donatrack.api.dto.DonacionDtos.SeleccionEntidadRequest;
import ar.edu.utn.donatrack.api.error.ApiException;
import ar.edu.utn.donatrack.api.mappers.ApiMapper;
import ar.edu.utn.donatrack.api.validation.DtoValidator;
import ar.edu.utn.donatrack.beneficiarios.BeneficiariosRepository;
import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.DonacionesRepository;
import ar.edu.utn.donatrack.notificaciones.ServicioNotificaciones;
import ar.edu.utn.donatrack.propuestas.EntidadFueraDeRankingException;
import ar.edu.utn.donatrack.propuestas.PropuestaDeAsignacion;
import ar.edu.utn.donatrack.propuestas.ServicioMatchmakingDonaciones;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.http.Context;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

public final class MatchmakingController implements WithSimplePersistenceUnit {
  private final ServicioMatchmakingDonaciones matchmaking;
  private final DonacionesRepository donaciones;
  private final BeneficiariosRepository entidades;
  private final ServicioNotificaciones notificaciones;
  private final Clock clock;

  public MatchmakingController(
      ServicioMatchmakingDonaciones matchmaking,
      DonacionesRepository donaciones,
      BeneficiariosRepository entidades,
      ServicioNotificaciones notificaciones,
      Clock clock) {
    this.matchmaking = matchmaking;
    this.donaciones = donaciones;
    this.entidades = entidades;
    this.notificaciones = notificaciones;
    this.clock = clock;
  }

  public void routes() {
    get("/propuestas-asignacion", this::listar);
    path(
        "/donaciones/{donacionId}",
        () -> {
          post("/propuesta-asignacion", this::ejecutar);
          get("/propuesta-asignacion", this::ranking);
          put("/propuesta-asignacion", this::seleccionar);
        });
  }

  private void listar(Context ctx) {
    ctx.json(matchmaking.obtenerPropuestasPendientes().stream().map(ApiMapper::propuesta).toList());
  }

  private void ejecutar(Context ctx) {
    ctx.status(201)
        .json(
            ApiMapper.propuesta(
                matchmaking.ejecutarMatchmakingParaDonacion(
                    donacion(HttpSupport.id(ctx, "donacionId")), LocalDate.now(clock))));
  }

  private void ranking(Context ctx) {
    ctx.json(ApiMapper.propuesta(ranking(HttpSupport.id(ctx, "donacionId"))));
  }

  private PropuestaDeAsignacion ranking(UUID donacionId) {
    try {
      return matchmaking.obtenerRankingVigente(donacion(donacionId));
    } catch (IllegalStateException error) {
      throw ApiException.notFound("Ranking de la donación", donacionId);
    }
  }

  private void seleccionar(Context ctx) {
    SeleccionEntidadRequest request = ctx.bodyAsClass(SeleccionEntidadRequest.class);
    Long entidadBeneficiariaId = DtoValidator.validate(request).entidadBeneficiariaId();
    DonacionResponse seleccionada =
        withTransaction(
            () ->
                ApiMapper.donacion(
                    confirmarSeleccion(HttpSupport.id(ctx, "donacionId"), entidadBeneficiariaId)));
    ctx.json(seleccionada);
  }

  private Donacion confirmarSeleccion(UUID donacionId, Long entidadId) {
    Donacion donacion = donacion(donacionId);
    EntidadBeneficiaria entidad =
        entidades
            .findById(entidadId)
            .orElseThrow(() -> ApiException.notFound("Entidad beneficiaria", entidadId));
    if (donacion.getEntidadBeneficiaria() != null) {
      if (donacion.getEntidadBeneficiaria().getId().equals(entidadId)) {
        return donacion;
      }
      throw ApiException.conflict("La donación ya tiene una entidad beneficiaria asignada.");
    }
    try {
      notificaciones.enviar(
          matchmaking.confirmarAsignacionAdministrativa(
              donacion, entidad, LocalDate.now(clock)));
    } catch (EntidadFueraDeRankingException error) {
      throw ApiException.conflict(error.getMessage());
    }
    return donacion;
  }

  private Donacion donacion(UUID id) {
    return donaciones.findById(id).orElseThrow(() -> ApiException.notFound("Donación", id));
  }
}
