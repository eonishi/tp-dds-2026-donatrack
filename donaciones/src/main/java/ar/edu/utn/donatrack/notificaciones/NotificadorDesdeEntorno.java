package ar.edu.utn.donatrack.notificaciones;

import ar.edu.utn.donatrack.notificaciones.infraestructura.GatewayCorreoResend;
import ar.edu.utn.donatrack.notificaciones.infraestructura.GatewayTwilio;
import ar.edu.utn.donatrack.notificaciones.infraestructura.GatewayWhatsappTwilio;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class NotificadorDesdeEntorno {
  private static final Logger LOG = Logger.getLogger(NotificadorDesdeEntorno.class.getName());
  private static final List<String> VARIABLES =
      List.of(
          "RESEND_API_KEY",
          "RESEND_FROM",
          "TWILIO_ACCOUNT_SID",
          "TWILIO_AUTH_TOKEN",
          "TWILIO_SMS_FROM",
          "TWILIO_WHATSAPP_FROM");

  private NotificadorDesdeEntorno() {}

  private static boolean ausente(Map<String, String> entorno, String variable) {
    return entorno.get(variable) == null || entorno.get(variable).isBlank();
  }

  public static Notificador crear() {
    return crear(System.getenv());
  }

  /**
   * Sin ninguna credencial usa el {@link NotificadorSimulado} y lo avisa en el log, para poder
   * levantar el sistema sin cuentas de Resend ni Twilio. Si hay algunas y faltan otras, falla: es
   * una configuración a medias que conviene notar al arrancar.
   */
  static Notificador crear(Map<String, String> entorno) {
    if (VARIABLES.stream().allMatch(variable -> ausente(entorno, variable))) {
      LOG.warning(
          "Sin credenciales de Resend ni Twilio: las notificaciones se simulan y no se envían.");
      return new NotificadorSimulado();
    }
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
    for (String variable : VARIABLES) {
      if (ausente(entorno, variable)) {
        faltantes.add(variable);
      }
    }
    if (!faltantes.isEmpty()) {
      throw new IllegalStateException(
          "Faltan variables de entorno requeridas: " + String.join(", ", faltantes));
    }
  }
}
