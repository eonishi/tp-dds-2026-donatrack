package ar.edu.utn.donatrack.notificaciones;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.Genero;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.Sms;
import ar.edu.utn.donatrack.donantes.TipoDocumento;
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
            new PersonaHumana(
                new CorreoElectronico("donante@example.org"),
                "Ana",
                "Pérez",
                30,
                "30111222",
                TipoDocumento.DNI,
                Genero.FEMENINO,
                "Calle 123"),
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
    PersonaHumana donante =
        new PersonaHumana(
            new CorreoElectronico("donante@example.org"),
            "Ana",
            "Pérez",
            30,
            "30111222",
            TipoDocumento.DNI,
            Genero.FEMENINO,
            "Calle 123");
    Sms predeterminado = new Sms("+541112345678");
    donante.addMedioDeContacto(predeterminado);
    donante.setContactoPredeterminado(predeterminado);
    Notificacion notificacion =
        new Notificacion(donante, "Tu donación fue asignada.", SinPayload.INSTANCE);

    assertDoesNotThrow(() -> servicio.enviar(notificacion));
    assertEquals(1, intentos.get());
  }
}
