package ar.edu.utn.donatrack.notificaciones;

import ar.edu.utn.donatrack.notificaciones.infraestructura.GatewayCorreoResend;
import ar.edu.utn.donatrack.notificaciones.infraestructura.GatewayTwilio;
import ar.edu.utn.donatrack.notificaciones.infraestructura.GatewayWhatsappTwilio;
import java.net.http.HttpClient;
import java.util.Map;

public final class NotificadorDesdeEntorno {
  private NotificadorDesdeEntorno() {}

  public static Notificador crear() {
    return crear(System.getenv());
  }

  static Notificador crear(Map<String, String> entorno) {
    HttpClient client = HttpClient.newHttpClient();
    GatewayCorreo correo =
        configurado(entorno, "RESEND_API_KEY", "RESEND_FROM")
            ? new GatewayCorreoResend(client, entorno.get("RESEND_API_KEY"), entorno.get("RESEND_FROM"))
            : (destinatario, asunto, mensaje) -> sinConfig("Resend");
    GatewaySms sms =
        configurado(entorno, "TWILIO_ACCOUNT_SID", "TWILIO_AUTH_TOKEN", "TWILIO_SMS_FROM")
            ? new GatewayTwilio(
                client,
                entorno.get("TWILIO_ACCOUNT_SID"),
                entorno.get("TWILIO_AUTH_TOKEN"),
                entorno.get("TWILIO_SMS_FROM"))
            : (destinatario, mensaje) -> sinConfig("Twilio SMS");
    GatewayWhatsapp whatsapp =
        configurado(
                entorno,
                "TWILIO_ACCOUNT_SID",
                "TWILIO_AUTH_TOKEN",
                "TWILIO_WHATSAPP_FROM")
            ? new GatewayWhatsappTwilio(
                client,
                entorno.get("TWILIO_ACCOUNT_SID"),
                entorno.get("TWILIO_AUTH_TOKEN"),
                entorno.get("TWILIO_WHATSAPP_FROM"))
            : (destinatario, mensaje) -> sinConfig("Twilio WhatsApp");
    return new NotificadorPorCanal(correo, sms, whatsapp);
  }

  private static boolean configurado(Map<String, String> entorno, String... variables) {
    for (String variable : variables) {
      if (entorno.get(variable) == null || entorno.get(variable).isBlank()) {
        return false;
      }
    }
    return true;
  }

  private static ResultadoEnvio sinConfig(String proveedor) {
    return ResultadoEnvio.fallido(proveedor + " no está configurado.");
  }
}
