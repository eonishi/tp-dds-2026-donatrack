package ar.edu.utn.donatrack.fotos;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

/**
 * Sube las fotos a un bucket público de Supabase Storage con su API REST.
 *
 * <p>La clave de servicio solo vive en el servidor: el navegador nunca habla con Supabase para
 * subir, solo para ver la foto por su URL pública.
 */
public final class AlmacenFotosSupabase implements AlmacenFotos {
  private final HttpClient cliente;
  private final String url;
  private final String claveServicio;
  private final String bucket;

  public AlmacenFotosSupabase(HttpClient cliente, String url, String claveServicio, String bucket) {
    this.cliente = Objects.requireNonNull(cliente);
    this.url = sinBarraFinal(Objects.requireNonNull(url));
    this.claveServicio = Objects.requireNonNull(claveServicio);
    this.bucket = Objects.requireNonNull(bucket);
  }

  @Override
  public String guardar(String nombre, String tipoContenido, byte[] contenido) throws IOException {
    HttpRequest pedido =
        HttpRequest.newBuilder(URI.create(url + "/storage/v1/object/" + bucket + "/" + nombre))
            .timeout(Duration.ofSeconds(30))
            .header("Authorization", "Bearer " + claveServicio)
            .header("apikey", claveServicio)
            .header("Content-Type", tipoContenido)
            .header("x-upsert", "false")
            .POST(HttpRequest.BodyPublishers.ofByteArray(contenido))
            .build();
    HttpResponse<String> respuesta;
    try {
      respuesta = cliente.send(pedido, HttpResponse.BodyHandlers.ofString());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Se interrumpió la subida de la foto.", e);
    }
    if (respuesta.statusCode() / 100 != 2) {
      throw new IOException(
          "Supabase Storage rechazó la foto (HTTP " + respuesta.statusCode() + ").");
    }
    return url + "/storage/v1/object/public/" + bucket + "/" + nombre;
  }

  private static String sinBarraFinal(String valor) {
    return valor.endsWith("/") ? valor.substring(0, valor.length() - 1) : valor;
  }
}
