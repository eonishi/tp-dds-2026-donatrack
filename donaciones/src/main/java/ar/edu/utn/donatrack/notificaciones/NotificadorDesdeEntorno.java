package ar.edu.utn.donatrack.notificaciones;

import ar.edu.utn.donatrack.notificaciones.infraestructura.GatewayCorreoResend;
import ar.edu.utn.donatrack.notificaciones.infraestructura.GatewayTwilio;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Arma el notificador a partir de las variables de entorno de los proveedores.
 *
 * <p>Cada canal se configura por separado: si le faltan variables, la aplicación igual arranca y
 * ese canal queda deshabilitado, devolviendo un envío fallido (que se registra en el log) en lugar
 * de contactar al proveedor. Solo se informan nombres de variables, nunca sus valores.
 */
public final class NotificadorDesdeEntorno {
  private static final Logger LOGGER = LoggerFactory.getLogger(NotificadorDesdeEntorno.class);

  private NotificadorDesdeEntorno() {}

  public static NotificadorPorCanal crear() {
    return crear(System.getenv());
  }

  static NotificadorPorCanal crear(Map<String, String> entorno) {
    HttpClient client = HttpClient.newHttpClient();
    return new NotificadorPorCanal(
        correo(entorno, client), sms(entorno, client), whatsapp(entorno, client));
  }

  static GatewayCorreo correo(Map<String, String> entorno, HttpClient client) {
    List<String> faltantes = faltantes(entorno, "RESEND_API_KEY", "RESEND_FROM");
    if (!faltantes.isEmpty()) {
      ResultadoEnvio deshabilitado = deshabilitado("CORREO", faltantes);
      return (destinatario, asunto, mensaje) -> deshabilitado;
    }
    return new GatewayCorreoResend(
        client, entorno.get("RESEND_API_KEY"), entorno.get("RESEND_FROM"));
  }

  static GatewayMensajes sms(Map<String, String> entorno, HttpClient client) {
    List<String> faltantes =
        faltantes(entorno, "TWILIO_ACCOUNT_SID", "TWILIO_AUTH_TOKEN", "TWILIO_SMS_FROM");
    if (!faltantes.isEmpty()) {
      ResultadoEnvio deshabilitado = deshabilitado("SMS", faltantes);
      return (destinatario, mensaje) -> deshabilitado;
    }
    return GatewayTwilio.sms(
        client,
        entorno.get("TWILIO_ACCOUNT_SID"),
        entorno.get("TWILIO_AUTH_TOKEN"),
        entorno.get("TWILIO_SMS_FROM"));
  }

  static GatewayMensajes whatsapp(Map<String, String> entorno, HttpClient client) {
    List<String> faltantes =
        faltantes(entorno, "TWILIO_ACCOUNT_SID", "TWILIO_AUTH_TOKEN", "TWILIO_WHATSAPP_FROM");
    if (!faltantes.isEmpty()) {
      ResultadoEnvio deshabilitado = deshabilitado("WHATSAPP", faltantes);
      return (destinatario, mensaje) -> deshabilitado;
    }
    return GatewayTwilio.whatsapp(
        client,
        entorno.get("TWILIO_ACCOUNT_SID"),
        entorno.get("TWILIO_AUTH_TOKEN"),
        entorno.get("TWILIO_WHATSAPP_FROM"));
  }

  private static List<String> faltantes(Map<String, String> entorno, String... variables) {
    return List.of(variables).stream()
        .filter(variable -> entorno.get(variable) == null || entorno.get(variable).isBlank())
        .toList();
  }

  private static ResultadoEnvio deshabilitado(String canal, List<String> faltantes) {
    String detalle =
        "Canal " + canal + " no configurado, faltan variables: " + String.join(", ", faltantes);
    LOGGER.warn(detalle);
    return ResultadoEnvio.fallido(detalle);
  }
}
