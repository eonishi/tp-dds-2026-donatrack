package ar.edu.utn.donatrack.notificaciones;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.MedioDeContacto;
import ar.edu.utn.donatrack.notificaciones.payloads.SinPayload;
import org.junit.jupiter.api.Test;

/** Verifies {@link Notificacion} state transitions: PENDIENTE → ENVIADA, PENDIENTE → FALLIDA. */
class NotificacionTest {

  private final MedioDeContacto medio = new CorreoElectronico("test@test.com");

  @Test
  void notificacionNueva_estadoInicialPendiente() {
    Notificacion notif =
        new Notificacion(medio, "msg", SinPayload.INSTANCE, EstadoNotificacion.PENDIENTE);

    assertEquals(EstadoNotificacion.PENDIENTE, notif.getEstado());
  }

  @Test
  void marcarEnviada_cambiaEstadoAEnviada() {
    Notificacion notif =
        new Notificacion(medio, "msg", SinPayload.INSTANCE, EstadoNotificacion.PENDIENTE);

    notif.marcarEnviada();

    assertEquals(EstadoNotificacion.ENVIADA, notif.getEstado());
  }

  @Test
  void marcarFallida_cambiaEstadoAFallida() {
    Notificacion notif =
        new Notificacion(medio, "msg", SinPayload.INSTANCE, EstadoNotificacion.PENDIENTE);

    notif.marcarFallida("El proveedor rechazó el envío.");

    assertEquals(EstadoNotificacion.FALLIDA, notif.getEstado());
  }
}
