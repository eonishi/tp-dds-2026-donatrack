package ar.edu.utn.donatrack.ubicacion;

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
 * Geocodifica con Nominatim (OpenStreetMap): gratis y sin clave, con la condición de identificarse
 * y no pasar de un pedido por segundo (lo respeta {@link UbicadorEntidades}).
 */
public class GeocodificadorNominatim implements Geocodificador {
  private static final String AGENTE = "DonaTrack/1.0 (TP Diseno de Sistemas, UTN FRBA)";

  private final HttpClient cliente;
  private final String url;
  private final ObjectMapper mapper = new ObjectMapper();

  public GeocodificadorNominatim(HttpClient cliente, String url) {
    this.cliente = cliente;
    this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }

  public static GeocodificadorNominatim desdeEntorno() {
    return new GeocodificadorNominatim(
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
        System.getenv().getOrDefault("NOMINATIM_URL", "https://nominatim.openstreetmap.org"));
  }

  @Override
  public Optional<Coordenadas> ubicar(String direccion) {
    // Las entidades son de Argentina: acotar el país evita homónimos de otros lugares.
    String consulta =
        url
            + "/search?format=json&limit=1&countrycodes=ar&q="
            + URLEncoder.encode(direccion, StandardCharsets.UTF_8);
    HttpRequest pedido =
        HttpRequest.newBuilder(URI.create(consulta))
            .timeout(Duration.ofSeconds(10))
            .header("User-Agent", AGENTE)
            .header("Accept-Language", "es")
            .GET()
            .build();
    try {
      HttpResponse<String> respuesta = cliente.send(pedido, HttpResponse.BodyHandlers.ofString());
      if (respuesta.statusCode() != 200) {
        throw new GeocodificacionNoDisponibleException(
            "Nominatim respondió " + respuesta.statusCode(), null);
      }
      JsonNode resultados = mapper.readTree(respuesta.body());
      if (!resultados.isArray() || resultados.isEmpty()) {
        return Optional.empty();
      }
      JsonNode primero = resultados.get(0);
      return Optional.of(
          new Coordenadas(primero.path("lat").asDouble(), primero.path("lon").asDouble()));
    } catch (IOException e) {
      throw new GeocodificacionNoDisponibleException("No se pudo consultar Nominatim", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new GeocodificacionNoDisponibleException("Se interrumpió la consulta a Nominatim", e);
    }
  }
}
