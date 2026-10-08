package ar.edu.utn.donatrack.logistica.planificacion;

import ar.edu.utn.donatrack.logistica.monitoreo.Coordenada;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DonacionesRemotasImpl implements DonacionesRemotas {

  private final String apiUrl;
  private final String apiKey;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  public DonacionesRemotasImpl(
      String apiUrl, String apiKey, HttpClient httpClient, ObjectMapper objectMapper) {
    this.apiUrl = apiUrl;
    this.apiKey = apiKey;
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
  }

  public record CoordenadaDto(double lat, double lng) {}

  public record DonacionResponseDto(
      UUID idDonacion,
      Long idEntidadBeneficiaria,
      String direccionDestino,
      CoordenadaDto coordenadas,
      Double volumenM3,
      Double pesoKg) {}

  @Override
  public List<DonacionPlanificable> listarEnAsignacionRealizada() {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(apiUrl + "/api/donaciones?estado=ASIGNACION_REALIZADA"))
            .header("Authorization", apiKey)
            .header("Accept", "application/json")
            .GET()
            .build();

    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      validarRespuesta(response);

      List<DonacionResponseDto> donaciones =
          objectMapper.readValue(response.body(), new TypeReference<>() {});

      return donaciones.stream()
          .map(
              dto ->
                  new DonacionPlanificable(
                      dto.idDonacion(),
                      dto.idEntidadBeneficiaria(),
                      dto.direccionDestino(),
                      new Coordenada(dto.coordenadas().lat(), dto.coordenadas().lng()),
                      dto.volumenM3() != null ? dto.volumenM3() : 0.0,
                      dto.pesoKg() != null ? dto.pesoKg() : 0.0))
          .toList();
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new RuntimeException("Error en la comunicación con servicio donaciones");
    }
  }

  @Override
  public void notificarListaParaEntregar(List<UUID> donaciones) {
    actualizarMasivo(donaciones, "LISTA_PARA_ENTREGAR", null);
  }

  @Override
  public void notificarInicioTraslado(List<UUID> donaciones, String idCamion) {
    actualizarMasivo(donaciones, "EN_TRASLADO", idCamion);
  }

  private void actualizarMasivo(List<UUID> donaciones, String nuevoEstado, String idCamion) {
    if (donaciones.isEmpty()) {
      return;
    }

    List<Map<String, Object>> payload =
        donaciones.stream()
            .map(
                d -> {
                  if (idCamion != null) {
                    return Map.of(
                        "idDonacion",
                        (Object) d.toString(),
                        "nuevoEstado",
                        (Object) nuevoEstado,
                        "idCamion",
                        (Object) idCamion);
                  }
                  return Map.of(
                      "idDonacion", (Object) d.toString(), "nuevoEstado", (Object) nuevoEstado);
                })
            .toList();

    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(apiUrl + "/api/donaciones/estado"))
              .header("Authorization", apiKey)
              .header("Content-Type", "application/json")
              .method(
                  "PATCH",
                  HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      validarRespuesta(response);
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new RuntimeException("Error al comunicarse con el servicio de donaciones", e);
    }
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
