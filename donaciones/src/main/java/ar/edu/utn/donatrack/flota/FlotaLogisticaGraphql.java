package ar.edu.utn.donatrack.flota;

import ar.edu.utn.donatrack.seguimiento.LogisticaNoDisponibleException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Cliente del contrato GraphQL de logística para la flota ({@code POST /logistica/graphql}). */
public class FlotaLogisticaGraphql implements FlotaLogistica {
  private static final String CAMPOS = "patente volumenM3 alturaM capacidadCargaKg";

  private final String logisticaUrl;
  private final HttpClient httpClient;
  private final ObjectMapper mapper = new ObjectMapper();

  public FlotaLogisticaGraphql(String logisticaUrl, HttpClient httpClient) {
    this.logisticaUrl = logisticaUrl;
    this.httpClient = httpClient;
  }

  public static FlotaLogisticaGraphql desdeEntorno() {
    return new FlotaLogisticaGraphql(
        System.getenv().getOrDefault("LOGISTICA_API_URL", "http://localhost:9001"),
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
  }

  @Override
  public List<Camion> camiones(String sesion) {
    JsonNode datos = ejecutar(sesion, "{ camiones { " + CAMPOS + " } }", Map.of());
    List<Camion> camiones = new ArrayList<>();
    datos.path("camiones").forEach(c -> camiones.add(camion(c)));
    return camiones;
  }

  @Override
  public Camion crear(String sesion, Camion camion) {
    return camion(
        ejecutar(
                sesion,
                "mutation($input: CamionInput!) { crearCamion(input: $input) { " + CAMPOS + " } }",
                Map.of("input", input(camion)))
            .path("crearCamion"));
  }

  @Override
  public Camion actualizar(String sesion, Camion camion) {
    return camion(
        ejecutar(
                sesion,
                "mutation($input: CamionInput!) { actualizarCamion(input: $input) { "
                    + CAMPOS
                    + " } }",
                Map.of("input", input(camion)))
            .path("actualizarCamion"));
  }

  @Override
  public boolean eliminar(String sesion, String patente) {
    return ejecutar(
            sesion,
            "mutation($patente: String!) { eliminarCamion(patente: $patente) }",
            Map.of("patente", patente))
        .path("eliminarCamion")
        .asBoolean();
  }

  private static Map<String, Object> input(Camion camion) {
    return Map.of(
        "patente", camion.getPatente(),
        "volumenM3", camion.getVolumenM3(),
        "alturaM", camion.getAlturaM(),
        "capacidadCargaKg", camion.getCapacidadCargaKg());
  }

  private static Camion camion(JsonNode nodo) {
    return new Camion(
        nodo.path("patente").asText(),
        nodo.path("volumenM3").asDouble(),
        nodo.path("alturaM").asDouble(),
        nodo.path("capacidadCargaKg").asDouble());
  }

  /**
   * @throws IllegalArgumentException con el mensaje de logística si rechazó los datos o el permiso
   * @throws LogisticaNoDisponibleException si logística no respondió
   */
  private JsonNode ejecutar(String sesion, String consulta, Map<String, Object> variables) {
    try {
      HttpRequest pedido =
          HttpRequest.newBuilder(URI.create(logisticaUrl + "/logistica/graphql"))
              .timeout(Duration.ofSeconds(10))
              .header("Content-Type", "application/json")
              // La misma cookie de sesión del navegador: logística valida el token de la persona.
              .header("Cookie", "access_token=" + sesion)
              .POST(
                  HttpRequest.BodyPublishers.ofString(
                      mapper.writeValueAsString(Map.of("query", consulta, "variables", variables))))
              .build();
      HttpResponse<String> respuesta =
          httpClient.send(pedido, HttpResponse.BodyHandlers.ofString());
      if (respuesta.statusCode() == 401 || respuesta.statusCode() == 403) {
        throw new IllegalArgumentException("Logística no aceptó tu sesión. Volvé a ingresar.");
      }
      if (respuesta.statusCode() >= 400) {
        throw new LogisticaNoDisponibleException(
            "Logística respondió " + respuesta.statusCode() + " al administrar la flota.");
      }
      JsonNode cuerpo = mapper.readTree(respuesta.body());
      JsonNode errores = cuerpo.path("errors");
      if (errores.isArray() && !errores.isEmpty()) {
        throw new IllegalArgumentException(errores.get(0).path("message").asText("Error."));
      }
      return cuerpo.path("data");
    } catch (IOException e) {
      throw new LogisticaNoDisponibleException("No se pudo contactar a logística", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new LogisticaNoDisponibleException("Se interrumpió la consulta a logística", e);
    }
  }
}
