package ar.edu.utn.donatrack.logistica.planificacion;

import ar.edu.utn.donatrack.logistica.Camion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;

/**
 * Implementación HTTP del puerto {@link PlanificadorRutas}: envía el {@link LoteDonaciones} al
 * componente externo de planificación y devuelve el identificador de lote que el proveedor asigna.
 *
 * <p>El resultado de la planificación no vuelve por esta llamada: llega después por el callback
 * {@code POST /logistica/planificacion/callback} (ver {@code PlanificacionController}).
 *
 * <p>La URL y la clave del proveedor se configuran desde el entorno; las pruebas usan un servidor
 * falso y no invocan al proveedor real.
 */
public class PlanificadorRutasHttp implements PlanificadorRutas {

  private final String apiUrl;
  private final String apiKey;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  public PlanificadorRutasHttp(
      String apiUrl, String apiKey, HttpClient httpClient, ObjectMapper objectMapper) {
    this.apiUrl = apiUrl;
    this.apiKey = apiKey;
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
  }

  /** Coordenada en el wire format del planificador (compatible con el contrato de donaciones). */
  public record CoordenadaDto(double lat, double lng) {}

  public record DonacionDto(
      UUID idDonacion,
      Long idEntidadBeneficiaria,
      String direccionDestino,
      CoordenadaDto coordenadas,
      double volumenM3,
      double pesoKg) {}

  public record CamionDto(
      String patente, double volumenM3, double alturaM, double capacidadCargaKg) {}

  public record LoteRequestDto(
      List<DonacionDto> donaciones, List<CamionDto> camiones, String urlCallback) {}

  public record LoteIdResponseDto(UUID loteId) {}

  @Override
  public UUID enviarLote(LoteDonaciones lote) {
    LoteRequestDto request =
        new LoteRequestDto(
            lote.donaciones().stream().map(this::mapearDonacion).toList(),
            lote.camiones().stream().map(this::mapearCamion).toList(),
            lote.urlCallback());

    try {
      HttpRequest httpRequest =
          HttpRequest.newBuilder()
              .uri(URI.create(apiUrl + "/lotes"))
              .header("Authorization", apiKey)
              .header("Content-Type", "application/json")
              .header("Accept", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
              .build();

      HttpResponse<String> response =
          httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
      validarRespuesta(response);

      LoteIdResponseDto resultado =
          objectMapper.readValue(response.body(), LoteIdResponseDto.class);
      if (resultado == null || resultado.loteId() == null) {
        throw new RuntimeException("Respuesta inválida del planificador externo: falta loteId");
      }
      return resultado.loteId();
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new RuntimeException("Error en la comunicación con el planificador externo", e);
    }
  }

  private DonacionDto mapearDonacion(DonacionPlanificable donacion) {
    return new DonacionDto(
        donacion.idDonacion(),
        donacion.idEntidadBeneficiaria(),
        donacion.direccionDestino(),
        new CoordenadaDto(donacion.coordenadas().latitud(), donacion.coordenadas().longitud()),
        donacion.volumenM3(),
        donacion.pesoKg());
  }

  private CamionDto mapearCamion(Camion camion) {
    return new CamionDto(
        camion.getPatente(),
        camion.getVolumenM3(),
        camion.getAlturaM(),
        camion.getCapacidadCargaKg());
  }

  private void validarRespuesta(HttpResponse<String> response) {
    if (response.statusCode() >= 400) {
      try {
        JsonNode problem = objectMapper.readTree(response.body());
        String detail = problem.has("detail") ? problem.get("detail").asText() : response.body();
        throw new RuntimeException("Error HTTP " + response.statusCode() + ": " + detail);
      } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
        throw new RuntimeException("Error HTTP " + response.statusCode() + ": " + response.body());
      }
    }
  }
}
