package ar.edu.utn.donatrack.seguimiento;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * Cliente HTTP de {@code GET /logistica/camiones/{patente}/posicion}, autenticado con la clave
 * interna entre servicios.
 */
public class PosicionesCamionesHttp implements PosicionesCamiones {
  private final String logisticaUrl;
  private final String apiKey;
  private final HttpClient httpClient;
  private final ObjectMapper mapper = new ObjectMapper();

  public PosicionesCamionesHttp(String logisticaUrl, String apiKey, HttpClient httpClient) {
    this.logisticaUrl = logisticaUrl;
    this.apiKey = apiKey;
    this.httpClient = httpClient;
  }

  public static PosicionesCamionesHttp desdeEntorno() {
    return new PosicionesCamionesHttp(
        System.getenv().getOrDefault("LOGISTICA_API_URL", "http://localhost:9001"),
        System.getenv().getOrDefault("DONATRACK_API_KEY", "S2S_SECRET_KEY"),
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
  }

  @Override
  public Optional<PosicionCamion> ultimaPosicion(String patente) {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    logisticaUrl
                        + "/logistica/camiones/"
                        + URLEncoder.encode(patente, StandardCharsets.UTF_8)
                        + "/posicion"))
            .timeout(Duration.ofSeconds(5))
            .header("Authorization", apiKey)
            .header("Accept", "application/json")
            .GET()
            .build();
    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 404) {
        return Optional.empty();
      }
      if (response.statusCode() >= 400) {
        throw new LogisticaNoDisponibleException(
            "Logística respondió " + response.statusCode() + " al consultar el camión " + patente);
      }
      JsonNode body = mapper.readTree(response.body());
      JsonNode coordenadas = body.get("coordenadas");
      return Optional.of(
          new PosicionCamion(
              patente,
              coordenadas.get("lat").asDouble(),
              coordenadas.get("lng").asDouble(),
              body.path("timestamp").asText(null)));
    } catch (IOException e) {
      throw new LogisticaNoDisponibleException("No se pudo consultar a logística", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new LogisticaNoDisponibleException("Se interrumpió la consulta a logística", e);
    }
  }
}
