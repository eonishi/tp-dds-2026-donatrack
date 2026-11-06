package ar.edu.utn.donatrack.notificaciones.infraestructura;

import ar.edu.utn.donatrack.notificaciones.GatewayMensajes;
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

/** Envía SMS o WhatsApp por la API de mensajes de Twilio; solo cambian remitente y prefijo. */
public class GatewayTwilio implements GatewayMensajes {
  private static final String PREFIJO_WHATSAPP = "whatsapp:";

  private final HttpClient client;
  private final String accountSid;
  private final String authToken;
  private final String authorization;
  private final String remitente;
  private final String prefijoDestinatario;

  private GatewayTwilio(
      HttpClient client,
      String accountSid,
      String authToken,
      String remitente,
      String prefijoDestinatario) {
    this.client = Objects.requireNonNull(client);
    this.accountSid = Objects.requireNonNull(accountSid);
    this.authToken = Objects.requireNonNull(authToken);
    this.authorization =
        "Basic "
            + Base64.getEncoder()
                .encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
    this.remitente = Objects.requireNonNull(remitente);
    this.prefijoDestinatario = prefijoDestinatario;
  }

  public static GatewayTwilio sms(
      HttpClient client, String accountSid, String authToken, String remitente) {
    return new GatewayTwilio(client, accountSid, authToken, remitente, "");
  }

  public static GatewayTwilio whatsapp(
      HttpClient client, String accountSid, String authToken, String remitente) {
    return new GatewayTwilio(client, accountSid, authToken, remitente, PREFIJO_WHATSAPP);
  }

  @Override
  public ResultadoEnvio enviar(String destinatario, String mensaje) {
    String form =
        "To=%s&From=%s&Body=%s"
            .formatted(valor(conPrefijo(destinatario)), valor(remitente), valor(mensaje));
    HttpRequest request =
        HttpRequest.newBuilder(
                URI.create(
                    "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json"))
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
      return ResultadoEnvio.fallido(
          RespuestaProveedor.error(
              "Twilio", response.statusCode(), response.body(), accountSid, authToken));
    } catch (IOException error) {
      return ResultadoEnvio.fallido("No se pudo conectar con Twilio: " + error.getMessage());
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      return ResultadoEnvio.fallido("El envío por Twilio fue interrumpido.");
    }
  }

  private String conPrefijo(String numero) {
    return numero.startsWith(prefijoDestinatario) ? numero : prefijoDestinatario + numero;
  }

  private String valor(String texto) {
    return URLEncoder.encode(texto, StandardCharsets.UTF_8);
  }
}
