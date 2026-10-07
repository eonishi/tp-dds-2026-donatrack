package ar.edu.utn.donatrack.notificaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.Sms;
import ar.edu.utn.donatrack.notificaciones.payloads.SinPayload;
import org.junit.jupiter.api.Test;

class ServicioNotificacionesTest {

  @Test
  void envioExitoso_marcaLaNotificacionComoEnviada() {
    GatewayCorreo correo = (destinatario, asunto, mensaje) -> ResultadoEnvio.aceptado();
    ServicioNotificaciones servicio =
        new ServicioNotificaciones(
            new NotificadorPorCanal(
                correo,
                (destinatario, mensaje) -> ResultadoEnvio.fallido("No corresponde"),
                (destinatario, mensaje) -> ResultadoEnvio.fallido("No corresponde")),
            new NotificacionesRepository(),
            3);
    Notificacion notificacion =
        new Notificacion(
            new CorreoElectronico("donante@example.org"),
            "Tu donación fue asignada.",
            SinPayload.INSTANCE,
            EstadoNotificacion.PENDIENTE);

    servicio.enviar(notificacion);

    assertEquals(EstadoNotificacion.ENVIADA, notificacion.getEstado());
    assertEquals(1, notificacion.getIntentos());
  }

  @Test
  void envioFallido_registraElErrorYReintenta() {
    GatewaySms sms =
        (destinatario, mensaje) -> ResultadoEnvio.fallido("Número no verificado en trial.");
    ServicioNotificaciones servicio =
        new ServicioNotificaciones(
            new NotificadorPorCanal(
                (destinatario, asunto, mensaje) -> ResultadoEnvio.fallido("No corresponde"),
                sms,
                (destinatario, mensaje) -> ResultadoEnvio.fallido("No corresponde")),
            new NotificacionesRepository(),
            3);
    Notificacion notificacion =
        new Notificacion(
            new Sms("+541112345678"),
            "Tu donación fue asignada.",
            SinPayload.INSTANCE,
            EstadoNotificacion.PENDIENTE);

    servicio.enviar(notificacion);

    assertEquals(EstadoNotificacion.FALLIDA, notificacion.getEstado());
    assertEquals(3, notificacion.getIntentos());
    assertEquals("Número no verificado en trial.", notificacion.getUltimoError());
  }
}
