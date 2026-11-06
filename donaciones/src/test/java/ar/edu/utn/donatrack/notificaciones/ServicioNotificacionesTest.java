package ar.edu.utn.donatrack.notificaciones;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.Sms;
import ar.edu.utn.donatrack.notificaciones.payloads.SinPayload;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ServicioNotificacionesTest {

  @Test
  void envioExitoso_invocaElGatewayInmediatamenteUnaSolaVez() {
    AtomicInteger envios = new AtomicInteger();
    AtomicReference<String> destinatarioRecibido = new AtomicReference<>();
    AtomicReference<String> mensajeRecibido = new AtomicReference<>();
    GatewayCorreo correo =
        (destinatario, asunto, mensaje) -> {
          envios.incrementAndGet();
          destinatarioRecibido.set(destinatario);
          mensajeRecibido.set(mensaje);
          return ResultadoEnvio.aceptado();
        };
    ServicioNotificaciones servicio =
        new ServicioNotificaciones(
            new NotificadorPorCanal(
                correo,
                (destinatario, mensaje) -> ResultadoEnvio.fallido("No corresponde"),
                (destinatario, mensaje) -> ResultadoEnvio.fallido("No corresponde")));
    Notificacion notificacion =
        new Notificacion(
            new CorreoElectronico("donante@example.org"),
            "Tu donación fue asignada.",
            SinPayload.INSTANCE);

    servicio.enviar(notificacion);

    assertEquals(1, envios.get());
    assertEquals("donante@example.org", destinatarioRecibido.get());
    assertEquals("Tu donación fue asignada.", mensajeRecibido.get());
  }

  @Test
  void envioFallido_intentaUnaSolaVezYNoPropagaExcepcion() {
    AtomicInteger intentos = new AtomicInteger();
    GatewaySms sms =
        (destinatario, mensaje) -> {
          intentos.incrementAndGet();
          return ResultadoEnvio.fallido("Número no verificado en trial.");
        };
    ServicioNotificaciones servicio =
        new ServicioNotificaciones(
            new NotificadorPorCanal(
                (destinatario, asunto, mensaje) -> ResultadoEnvio.fallido("No corresponde"),
                sms,
                (destinatario, mensaje) -> ResultadoEnvio.fallido("No corresponde")));
    Notificacion notificacion =
        new Notificacion(
            new Sms("+541112345678"), "Tu donación fue asignada.", SinPayload.INSTANCE);

    assertDoesNotThrow(() -> servicio.enviar(notificacion));
    assertEquals(1, intentos.get());
  }
}
