package ar.edu.utn.donatrack.notificaciones;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.MedioDeContacto;
import ar.edu.utn.donatrack.notificaciones.payloads.SinPayload;
import org.junit.jupiter.api.Test;

/** Verifies {@link Notificacion} construction and invariants. */
class NotificacionTest {

  private final MedioDeContacto medio = new CorreoElectronico("test@test.com");

  @Test
  void notificacionNueva_conservaDestinatarioMensajePayloadYFechaCreacion() {
    Notificacion notif = new Notificacion(medio, "msg", SinPayload.INSTANCE);

    assertEquals(medio, notif.getDestinatario());
    assertEquals("msg", notif.getMensaje());
    assertEquals(SinPayload.INSTANCE, notif.getPayload());
    assertNotNull(notif.getFechaCreacion());
  }

  @Test
  void constructor_rechazaArgumentosNulos() {
    assertThrows(
        NullPointerException.class, () -> new Notificacion(null, "msg", SinPayload.INSTANCE));
    assertThrows(
        NullPointerException.class, () -> new Notificacion(medio, null, SinPayload.INSTANCE));
    assertThrows(NullPointerException.class, () -> new Notificacion(medio, "msg", null));
  }
}
