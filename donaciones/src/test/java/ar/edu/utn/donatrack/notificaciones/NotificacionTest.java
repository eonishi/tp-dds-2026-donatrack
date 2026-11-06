package ar.edu.utn.donatrack.notificaciones;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.Genero;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.TipoDocumento;
import ar.edu.utn.donatrack.notificaciones.payloads.SinPayload;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies {@link Notificacion} construction, invariants and delegation to its recipient. */
class NotificacionTest {
  private final PersonaHumana donante =
      new PersonaHumana(
          new CorreoElectronico("donante@test.com"),
          "Ana",
          "Pérez",
          30,
          "30111222",
          TipoDocumento.DNI,
          Genero.FEMENINO,
          "Calle 123");

  @Test
  void notificacionNueva_conservaDestinatarioMensajePayloadYFechaCreacion() {
    Notificacion notif = new Notificacion(donante, "msg", SinPayload.INSTANCE);

    assertSame(donante, notif.getDestinatario());
    assertEquals("msg", notif.getMensaje());
    assertEquals(SinPayload.INSTANCE, notif.getPayload());
    assertNotNull(notif.getFechaCreacion());
    assertTrue(notif.getEnvios().isEmpty());
  }

  @Test
  void enviarCon_delegaEnElDestinatarioYRegistraElCanal() {
    List<String> correos = new ArrayList<>();
    NotificadorPorCanal canales =
        new NotificadorPorCanal(
            (destinatario, asunto, mensaje) -> {
              correos.add(destinatario + "|" + mensaje);
              return ResultadoEnvio.aceptado();
            },
            (destinatario, mensaje) -> ResultadoEnvio.fallido("No corresponde"),
            (destinatario, mensaje) -> ResultadoEnvio.fallido("No corresponde"));
    Notificacion notif = new Notificacion(donante, "Hola", SinPayload.INSTANCE);

    List<Envio> envios = notif.enviarCon(canales);

    assertEquals(List.of("donante@test.com|Hola"), correos);
    assertEquals(List.of(new Envio(Canal.CORREO, ResultadoEnvio.aceptado())), envios);
    assertEquals(envios, notif.getEnvios());
  }

  @Test
  void constructor_rechazaArgumentosNulos() {
    assertThrows(
        NullPointerException.class, () -> new Notificacion(null, "msg", SinPayload.INSTANCE));
    assertThrows(
        NullPointerException.class, () -> new Notificacion(donante, null, SinPayload.INSTANCE));
    assertThrows(NullPointerException.class, () -> new Notificacion(donante, "msg", null));
  }
}
