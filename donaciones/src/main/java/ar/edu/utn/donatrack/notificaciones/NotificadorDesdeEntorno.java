package ar.edu.utn.donatrack.notificaciones;

import ar.edu.utn.donatrack.notificaciones.infraestructura.GatewayCorreoResend;
import ar.edu.utn.donatrack.notificaciones.infraestructura.GatewayTwilio;
import ar.edu.utn.donatrack.notificaciones.infraestructura.GatewayWhatsappTwilio;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class NotificadorDesdeEntorno {
  private NotificadorDesdeEntorno() {}

  public static Notificador crear() {
    return crear(System.getenv());
  }

  static Notificador crear(Map<String, String> entorno) {
    validarConfiguracion(entorno);
    HttpClient client = HttpClient.newHttpClient();
    GatewayCorreo correo =
        new GatewayCorreoResend(client, entorno.get("RESEND_API_KEY"), entorno.get("RESEND_FROM"));
    GatewaySms sms =
        new GatewayTwilio(
            client,
            entorno.get("TWILIO_ACCOUNT_SID"),
            entorno.get("TWILIO_AUTH_TOKEN"),
            entorno.get("TWILIO_SMS_FROM"));
    GatewayWhatsapp whatsapp =
        new GatewayWhatsappTwilio(
            client,
            entorno.get("TWILIO_ACCOUNT_SID"),
            entorno.get("TWILIO_AUTH_TOKEN"),
            entorno.get("TWILIO_WHATSAPP_FROM"));
    return new NotificadorPorCanal(correo, sms, whatsapp);
  }

  private static void validarConfiguracion(Map<String, String> entorno) {
    List<String> faltantes = new ArrayList<>();
    for (String variable :
        List.of(
            "RESEND_API_KEY",
            "RESEND_FROM",
            "TWILIO_ACCOUNT_SID",
            "TWILIO_AUTH_TOKEN",
            "TWILIO_SMS_FROM",
            "TWILIO_WHATSAPP_FROM")) {
      if (entorno.get(variable) == null || entorno.get(variable).isBlank()) {
        faltantes.add(variable);
      }
    }
    if (!faltantes.isEmpty()) {
      throw new IllegalStateException(
          "Faltan variables de entorno requeridas: " + String.join(", ", faltantes));
    }
  }
}
