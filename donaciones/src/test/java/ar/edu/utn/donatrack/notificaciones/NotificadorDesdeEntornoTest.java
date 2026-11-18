package ar.edu.utn.donatrack.notificaciones;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.donatrack.notificaciones.infraestructura.GatewayCorreoResend;
import ar.edu.utn.donatrack.notificaciones.infraestructura.GatewayTwilio;
import java.net.http.HttpClient;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NotificadorDesdeEntornoTest {
  private static final HttpClient CLIENT = HttpClient.newHttpClient();

  @Test
  void arrancaSinNingunaVariableDeProveedores() {
    assertDoesNotThrow(() -> NotificadorDesdeEntorno.crear(Map.of()));
  }

  @Test
  void canalSinConfigurarFallaSinEnviarInformandoNombresYSinExponerValores() {
    Map<String, String> entorno =
        Map.of("RESEND_API_KEY", "secreto-no-exponer", "TWILIO_ACCOUNT_SID", "sid");

    ResultadoEnvio correo =
        NotificadorDesdeEntorno.correo(entorno, CLIENT).enviar("a@b.com", "asunto", "hola");
    ResultadoEnvio sms = NotificadorDesdeEntorno.sms(entorno, CLIENT).enviar("+54911", "hola");
    ResultadoEnvio whatsapp =
        NotificadorDesdeEntorno.whatsapp(entorno, CLIENT).enviar("+54911", "hola");

    assertFalse(correo.exitoso());
    assertTrue(correo.detalle().contains("RESEND_FROM"));
    assertFalse(correo.detalle().contains("secreto-no-exponer"));
    assertFalse(sms.exitoso());
    assertTrue(sms.detalle().contains("TWILIO_AUTH_TOKEN"));
    assertTrue(sms.detalle().contains("TWILIO_SMS_FROM"));
    assertFalse(whatsapp.exitoso());
    assertTrue(whatsapp.detalle().contains("TWILIO_WHATSAPP_FROM"));
  }

  @Test
  void usaCadaProveedorRealCuandoSuCanalEstaConfigurado() {
    Map<String, String> entorno =
        Map.of(
            "RESEND_API_KEY", "key",
            "RESEND_FROM", "DonaTrack <a@b.com>",
            "TWILIO_ACCOUNT_SID", "sid",
            "TWILIO_AUTH_TOKEN", "token",
            "TWILIO_SMS_FROM", "+1555",
            "TWILIO_WHATSAPP_FROM", "whatsapp:+1555");

    assertInstanceOf(GatewayCorreoResend.class, NotificadorDesdeEntorno.correo(entorno, CLIENT));
    assertInstanceOf(GatewayTwilio.class, NotificadorDesdeEntorno.sms(entorno, CLIENT));
    assertInstanceOf(GatewayTwilio.class, NotificadorDesdeEntorno.whatsapp(entorno, CLIENT));
  }

  @Test
  void configuraCanalesIndependientemente() {
    Map<String, String> entorno =
        Map.of(
            "TWILIO_ACCOUNT_SID", "sid",
            "TWILIO_AUTH_TOKEN", "token",
            "TWILIO_SMS_FROM", "+1555",
            "TWILIO_WHATSAPP_FROM", " ");

    assertInstanceOf(GatewayTwilio.class, NotificadorDesdeEntorno.sms(entorno, CLIENT));
    assertFalse(
        NotificadorDesdeEntorno.whatsapp(entorno, CLIENT).enviar("+54911", "hola").exitoso());
    assertFalse(
        NotificadorDesdeEntorno.correo(entorno, CLIENT).enviar("a@b.com", "s", "m").exitoso());
  }
}
