package ar.edu.utn.donatrack.notificaciones.infraestructura;

import ar.edu.utn.donatrack.notificaciones.GatewaySms;
import ar.edu.utn.donatrack.notificaciones.ResultadoEnvio;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;

public class GatewayTwilio implements GatewaySms {
  private final HttpClient client;
  private final String accountSid;
  private final String authorization;
  private final String smsRemitente;

  public GatewayTwilio(
      HttpClient client,
      String accountSid,
      String authToken,
      String smsRemitente) {
    this.client = Objects.requireNonNull(client);
    this.accountSid = Objects.requireNonNull(accountSid);
    this.authorization =
        "Basic "
            + Base64.getEncoder()
                .encodeToString((accountSid + ":" + Objects.requireNonNull(authToken)).getBytes(StandardCharsets.UTF_8));
    this.smsRemitente = Objects.requireNonNull(smsRemitente);
  }

  @Override
  public ResultadoEnvio enviar(String destinatario, String mensaje) {
    return enviarMensaje(smsRemitente, destinatario, mensaje);
  }

  private ResultadoEnvio enviarMensaje(String remitente, String destinatario, String mensaje) {
    String form = "To=%s&From=%s&Body=%s".formatted(valor(destinatario), valor(remitente), valor(mensaje));
    HttpRequest request =
        HttpRequest.newBuilder(
                URI.create(
                    "https://api.twilio.com/2010-04-01/Accounts/"
                        + accountSid
                        + "/Messages.json"))
            .header("Authorization", authorization)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .timeout(Duration.ofSeconds(10))
            .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
            .build();
    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 200 && response.statusCode() < 300) {
        return ResultadoEnvio.aceptado();
      }
      return ResultadoEnvio.fallido("Twilio respondió HTTP " + response.statusCode() + ".");
    } catch (IOException error) {
      return ResultadoEnvio.fallido("No se pudo conectar con Twilio: " + error.getMessage());
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      return ResultadoEnvio.fallido("El envío por Twilio fue interrumpido.");
    }
  }

  private String valor(String texto) {
    return URLEncoder.encode(texto, StandardCharsets.UTF_8);
  }
}
