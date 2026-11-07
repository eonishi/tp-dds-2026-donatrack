package ar.edu.utn.donatrack.notificaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.Genero;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.Sms;
import ar.edu.utn.donatrack.donantes.TipoDocumento;
import ar.edu.utn.donatrack.notificaciones.eventos.EventoBienvenida;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class NotificadorPorCanalTest {
  @Test
  void usaSolamenteElGatewayDelMedioPredeterminado() {
    AtomicInteger correos = new AtomicInteger();
    AtomicInteger sms = new AtomicInteger();
    AtomicInteger whatsapp = new AtomicInteger();
    NotificadorPorCanal notificador =
        new NotificadorPorCanal(
            (destinatario, asunto, mensaje) -> {
              correos.incrementAndGet();
              return ResultadoEnvio.aceptado();
            },
            (destinatario, mensaje) -> {
              sms.incrementAndGet();
              return ResultadoEnvio.aceptado();
            },
            (destinatario, mensaje) -> {
              whatsapp.incrementAndGet();
              return ResultadoEnvio.aceptado();
            });
    PersonaHumana donante =
        new PersonaHumana(
            new CorreoElectronico("donante@example.org"),
            "Ana",
            "Pérez",
            "30111222",
            TipoDocumento.DNI,
            Genero.FEMENINO,
            "Calle 123");
    Sms predeterminado = new Sms("+541112345678");
    donante.addMedioDeContacto(predeterminado);
    donante.setContactoPredeterminado(predeterminado);

    new EventoBienvenida(donante)
        .generarNotificaciones()
        .forEach(n -> notificador.enviarNotificacion(n.getDestinatario(), n.getMensaje()));

    assertEquals(0, correos.get());
    assertEquals(1, sms.get());
    assertEquals(0, whatsapp.get());
  }
}
