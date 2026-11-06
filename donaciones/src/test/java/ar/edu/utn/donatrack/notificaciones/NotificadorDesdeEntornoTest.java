package ar.edu.utn.donatrack.notificaciones;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class NotificadorDesdeEntornoTest {
  @Test
  void informaNombresDeVariablesFaltantesSinExponerValores() {
    IllegalStateException error =
        assertThrows(
            IllegalStateException.class,
            () ->
                NotificadorDesdeEntorno.crear(
                    Map.of("RESEND_API_KEY", "secreto-no-exponer", "TWILIO_ACCOUNT_SID", "sid")));

    assertTrue(error.getMessage().contains("RESEND_FROM"));
    assertTrue(error.getMessage().contains("TWILIO_AUTH_TOKEN"));
    assertTrue(error.getMessage().contains("TWILIO_SMS_FROM"));
    assertTrue(error.getMessage().contains("TWILIO_WHATSAPP_FROM"));
    assertFalse(error.getMessage().contains("secreto-no-exponer"));
  }
}
