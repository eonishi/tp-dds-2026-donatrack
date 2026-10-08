package ar.edu.utn.donatrack.logistica.controllers;

import ar.edu.utn.donatrack.logistica.Camion;
import ar.edu.utn.donatrack.logistica.CamionRepository;
import ar.edu.utn.donatrack.logistica.Destino;
import ar.edu.utn.donatrack.logistica.planificacion.ResultadoPlanificacion;
import ar.edu.utn.donatrack.logistica.planificacion.ServicioPlanificacionRutas;
import io.javalin.http.Context;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlanificacionController {

  private final ServicioPlanificacionRutas servicio;
  private final CamionRepository camionRepo;

  public PlanificacionController(ServicioPlanificacionRutas servicio, CamionRepository camionRepo) {
    this.servicio = servicio;
    this.camionRepo = camionRepo;
  }

  public void procesarResultado(Context ctx) {
    ResultadoPlanificacionDto dto;
    try {
      dto = ctx.bodyAsClass(ResultadoPlanificacionDto.class);
    } catch (Exception e) {
      ctx.status(400).result(mensajeDe(e, "Payload de resultado inválido"));
      return;
    }

    ResultadoPlanificacion resultado;
    try {
      resultado = mapearResultado(dto);
    } catch (IllegalArgumentException e) {
      ctx.status(400).result(mensajeDe(e, "Resultado inválido"));
      return;
    }

    try {
      servicio.procesarResultado(resultado);
      ctx.status(200);
    } catch (IllegalStateException e) {
      ctx.status(404).result(mensajeDe(e, "Lote no encontrado"));
    } catch (IllegalArgumentException e) {
      ctx.status(400).result(mensajeDe(e, "Resultado inválido"));
    }
  }

  private ResultadoPlanificacion mapearResultado(ResultadoPlanificacionDto dto) {
    if (dto == null || dto.loteId == null || dto.rutas == null) {
      throw new IllegalArgumentException("El resultado debe incluir loteId y rutas");
    }

    Map<Camion, List<Destino>> rutas = new LinkedHashMap<>();
    for (Map.Entry<String, List<DestinoDto>> entry : dto.rutas.entrySet()) {
      Camion camion =
          camionRepo
              .buscarPorPatente(entry.getKey())
              .orElseThrow(
                  () -> new IllegalArgumentException("Camión inexistente: " + entry.getKey()));
      List<DestinoDto> destinos = entry.getValue() == null ? List.of() : entry.getValue();
      rutas.put(camion, destinos.stream().map(this::mapearDestino).toList());
    }

    return new ResultadoPlanificacion(
        UUID.fromString(dto.loteId), rutas, mapearDonaciones(dto.donacionesNoAsignadas));
  }

  private Destino mapearDestino(DestinoDto dto) {
    if (dto == null || dto.entidadBeneficiariaId == null || dto.direccion == null) {
      throw new IllegalArgumentException(
          "Cada destino debe incluir entidadBeneficiariaId y direccion");
    }
    return new Destino(dto.entidadBeneficiariaId, dto.direccion, mapearDonaciones(dto.donaciones));
  }

  private List<UUID> mapearDonaciones(List<String> valores) {
    if (valores == null) {
      return List.of();
    }
    return valores.stream().map(UUID::fromString).toList();
  }

  private String mensajeDe(Exception e, String porDefecto) {
    return e.getMessage() == null ? porDefecto : e.getMessage();
  }

  public static class ResultadoPlanificacionDto {
    public String loteId;
    public Map<String, List<DestinoDto>> rutas;
    public List<String> donacionesNoAsignadas;
  }

  public static class DestinoDto {
    public Long entidadBeneficiariaId;
    public String direccion;
    public List<String> donaciones;
  }
}
