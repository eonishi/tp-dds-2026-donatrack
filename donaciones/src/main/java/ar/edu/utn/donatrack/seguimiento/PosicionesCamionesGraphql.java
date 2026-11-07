package ar.edu.utn.donatrack.seguimiento;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Cliente de la consulta {@code posicion} del contrato GraphQL de logística ({@code POST
 * /logistica/graphql}), autenticado con la clave interna entre servicios.
 */
public class PosicionesCamionesGraphql implements PosicionesCamiones {
  private static final String CONSULTA =
      "query($patente: String!) { posicion(patente: $patente) {"
          + " patente latitud longitud actualizadaEn } }";

  private final String logisticaUrl;
  private final String apiKey;
  private final HttpClient httpClient;
  private final ObjectMapper mapper = new ObjectMapper();

  public PosicionesCamionesGraphql(String logisticaUrl, String apiKey, HttpClient httpClient) {
    this.logisticaUrl = logisticaUrl;
    this.apiKey = apiKey;
    this.httpClient = httpClient;
  }

  public static PosicionesCamionesGraphql desdeEntorno() {
    return new PosicionesCamionesGraphql(
        System.getenv().getOrDefault("LOGISTICA_API_URL", "http://localhost:9001"),
        System.getenv().getOrDefault("DONATRACK_API_KEY", ""),
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
  }

  @Override
  public Optional<PosicionCamion> ultimaPosicion(String patente) {
    try {
      String cuerpo =
          mapper.writeValueAsString(
              Map.of("query", CONSULTA, "variables", Map.of("patente", patente)));
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(logisticaUrl + "/logistica/graphql"))
              .timeout(Duration.ofSeconds(5))
              .header("Authorization", apiKey)
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 400) {
        throw new LogisticaNoDisponibleException(
            "Logística respondió " + response.statusCode() + " al consultar el camión " + patente);
      }
      JsonNode resultado = mapper.readTree(response.body());
      JsonNode errores = resultado.path("errors");
      if (errores.isArray() && errores.size() > 0) {
        // Un camión que logística no conoce no tiene posición; cualquier otro error es de
        // logística.
        if ("NO_ENCONTRADO".equals(errores.get(0).path("extensions").path("codigo").asText())) {
          return Optional.empty();
        }
        throw new LogisticaNoDisponibleException(
            "Logística no pudo informar la posición: " + errores.get(0).path("message").asText());
      }
      JsonNode posicion = resultado.path("data").path("posicion");
      if (posicion.isMissingNode() || posicion.isNull()) {
        return Optional.empty();
      }
      return Optional.of(
          new PosicionCamion(
              patente,
              posicion.path("latitud").asDouble(),
              posicion.path("longitud").asDouble(),
              posicion.path("actualizadaEn").asText(null)));
    } catch (IOException e) {
      throw new LogisticaNoDisponibleException("No se pudo consultar a logística", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new LogisticaNoDisponibleException("Se interrumpió la consulta a logística", e);
    }
  }
}
