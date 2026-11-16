package ar.edu.utn.donatrack.notificaciones;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.Genero;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.TipoDocumento;
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
          "30111222",
          TipoDocumento.DNI,
          Genero.FEMENINO,
          "Calle 123");

  @Test
  void notificacionNueva_conservaDestinatarioYMensaje() {
    Notificacion notif = new Notificacion(donante, "msg");

    assertSame(donante, notif.destinatario());
    assertEquals("msg", notif.mensaje());
  }

  @Test
  void enviarCon_delegaEnElDestinatario() {
    List<String> correos = new ArrayList<>();
    NotificadorPorCanal canales =
        new NotificadorPorCanal(
            (destinatario, asunto, mensaje) -> {
              correos.add(destinatario + "|" + mensaje);
              return ResultadoEnvio.aceptado();
            },
            (destinatario, mensaje) -> ResultadoEnvio.fallido("No corresponde"),
            (destinatario, mensaje) -> ResultadoEnvio.fallido("No corresponde"));
    Notificacion notif = new Notificacion(donante, "Hola");

    notif.enviarCon(canales);

    assertEquals(List.of("donante@test.com|Hola"), correos);
  }

  @Test
  void constructor_rechazaArgumentosNulos() {
    assertThrows(NullPointerException.class, () -> new Notificacion(null, "msg"));
    assertThrows(NullPointerException.class, () -> new Notificacion(donante, null));
  }
}
