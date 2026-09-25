package ar.edu.utn.donatrack.notificaciones.infraestructura;

import ar.edu.utn.donatrack.notificaciones.GatewayCorreo;
import ar.edu.utn.donatrack.notificaciones.ResultadoEnvio;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

public class GatewayCorreoResend implements GatewayCorreo {
  private static final URI EMAILS_URI = URI.create("https://api.resend.com/emails");
  private final HttpClient client;
  private final String apiKey;
  private final String remitente;

  public GatewayCorreoResend(HttpClient client, String apiKey, String remitente) {
    this.client = Objects.requireNonNull(client);
    this.apiKey = Objects.requireNonNull(apiKey);
    this.remitente = Objects.requireNonNull(remitente);
  }

  @Override
  public ResultadoEnvio enviar(String destinatario, String asunto, String mensaje) {
    String body =
        "{\"from\":\"%s\",\"to\":[\"%s\"],\"subject\":\"%s\",\"text\":\"%s\"}"
            .formatted(
                escapar(remitente), escapar(destinatario), escapar(asunto), escapar(mensaje));
    HttpRequest request =
        HttpRequest.newBuilder(EMAILS_URI)
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .header("User-Agent", "DonaTrack/1.0")
            .timeout(Duration.ofSeconds(10))
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build();
    return ejecutar(request);
  }

  private ResultadoEnvio ejecutar(HttpRequest request) {
    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 200 && response.statusCode() < 300) {
        return ResultadoEnvio.aceptado();
      }
      return ResultadoEnvio.fallido(
          RespuestaProveedor.error("Resend", response.statusCode(), response.body(), apiKey));
    } catch (IOException error) {
      return ResultadoEnvio.fallido("No se pudo conectar con Resend: " + error.getMessage());
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      return ResultadoEnvio.fallido("El envío por Resend fue interrumpido.");
    }
  }

  private String escapar(String valor) {
    return valor.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
  }
}
