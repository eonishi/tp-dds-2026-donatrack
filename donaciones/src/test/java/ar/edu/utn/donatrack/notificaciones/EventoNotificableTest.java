package ar.edu.utn.donatrack.notificaciones;

import static ar.edu.utn.donatrack.testsupport.FabricaDePrueba.nuevaEntidad;
import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.Genero;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.Sms;
import ar.edu.utn.donatrack.donantes.TipoDocumento;
import ar.edu.utn.donatrack.notificaciones.eventos.EventoAsignacion;
import ar.edu.utn.donatrack.notificaciones.eventos.EventoAusenciaDonante;
import ar.edu.utn.donatrack.notificaciones.eventos.EventoBienvenida;
import ar.edu.utn.donatrack.notificaciones.eventos.EventoEntregaExitosa;
import ar.edu.utn.donatrack.notificaciones.eventos.EventoEntregaNoSatisfactoria;
import ar.edu.utn.donatrack.notificaciones.eventos.EventoInicioRuta;
import ar.edu.utn.donatrack.notificaciones.payloads.ComprobanteEntrega;
import ar.edu.utn.donatrack.notificaciones.payloads.InfoMapa;
import ar.edu.utn.donatrack.notificaciones.payloads.NotificacionPayload;
import ar.edu.utn.donatrack.notificaciones.payloads.SinPayload;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Verifies that each {@code EventoNotificable} subclass generates notifications with the correct
 * payload type and recipients.
 */
class EventoNotificableTest {

  private final PersonaHumana donante =
      new PersonaHumana(
          new CorreoElectronico("donante@test.com"),
          "Juan",
          "Perez",
          30,
          "12345678",
          TipoDocumento.DNI,
          Genero.MASCULINO,
          "Calle Falsa 123");

  private final EntidadBeneficiaria entidad = nuevaEntidad("Fundación Test");

  @Test
  void eventoBienvenida_generaNotificaciones_conSinPayload() {
    entidad.agregarCorreo(new CorreoElectronico("entidad@test.com"));
    EventoBienvenida evento = new EventoBienvenida(donante);

    List<Notificacion> notificaciones = evento.generarNotificaciones();

    assertFalse(notificaciones.isEmpty());
    for (Notificacion n : notificaciones) {
      assertInstanceOf(SinPayload.class, n.getPayload());
      assertEquals(EstadoNotificacion.PENDIENTE, n.getEstado());
    }
  }

  @Test
  void eventoEntregaExitosa_generaNotificaciones_conComprobante() {
    entidad.agregarCorreo(new CorreoElectronico("entidad@test.com"));
    entidad.agregarCorreo(new CorreoElectronico("otro@test.com"));
    LocalDateTime fecha = LocalDateTime.of(2026, 7, 10, 15, 0);
    EventoEntregaExitosa evento = new EventoEntregaExitosa(donante, entidad, fecha, "ABC123");

    List<Notificacion> notificaciones = evento.generarNotificaciones();

    assertFalse(notificaciones.isEmpty());
    for (Notificacion n : notificaciones) {
      NotificacionPayload payload = n.getPayload();
      assertInstanceOf(ComprobanteEntrega.class, payload);
      ComprobanteEntrega comprobante = (ComprobanteEntrega) payload;
      assertEquals(fecha, comprobante.fechaEntrega());
      assertEquals("ABC123", comprobante.idCamion());
    }
  }

  @Test
  void eventoEntregaExitosa_SinCamion_DebeLanzarNullPointerException() {
    LocalDateTime fecha = LocalDateTime.of(2026, 7, 10, 15, 0);

    assertThrows(
        NullPointerException.class, () -> new EventoEntregaExitosa(donante, entidad, fecha, null));
  }

  @Test
  void comprobanteEntrega_SinCamion_DebeLanzarNullPointerException() {
    LocalDateTime fecha = LocalDateTime.of(2026, 7, 10, 15, 0);

    assertThrows(NullPointerException.class, () -> new ComprobanteEntrega(fecha, null));
  }

  @Test
  void eventoAsignacion_generaNotificaciones_paraDonanteYEntidad() {
    entidad.agregarCorreo(new CorreoElectronico("entidad@test.com"));
    EventoAsignacion evento = new EventoAsignacion(donante, entidad);

    List<Notificacion> notificaciones = evento.generarNotificaciones();

    assertEquals(2, notificaciones.size());
    for (Notificacion n : notificaciones) {
      assertInstanceOf(SinPayload.class, n.getPayload());
      assertEquals(EstadoNotificacion.PENDIENTE, n.getEstado());
    }
  }

  @Test
  void eventoAsignacion_usaSoloElContactoPredeterminadoDelDonante() {
    donante.addMedioDeContacto(new Sms("+541112345678"));
    donante.setContactoPredeterminado(new Sms("+541112345678"));
    entidad.agregarCorreo(new CorreoElectronico("entidad@test.com"));

    List<Notificacion> notificaciones =
        new EventoAsignacion(donante, entidad).generarNotificaciones();

    assertEquals(2, notificaciones.size());
    assertInstanceOf(Sms.class, notificaciones.get(0).getDestinatario());
  }

  @Test
  void eventoAusenciaDonante_generaNotificaciones_paraDonante() {
    EventoAusenciaDonante evento = new EventoAusenciaDonante(donante);

    List<Notificacion> notificaciones = evento.generarNotificaciones();

    assertFalse(notificaciones.isEmpty());
    for (Notificacion n : notificaciones) {
      assertInstanceOf(SinPayload.class, n.getPayload());
      assertEquals(EstadoNotificacion.PENDIENTE, n.getEstado());
      assertTrue(n.getMensaje().contains("20 días"));
    }
  }

  @Test
  void eventoEntregaNoSatisfactoria_generaNotificaciones_paraDonanteYEntidad() {
    entidad.agregarCorreo(new CorreoElectronico("entidad@test.com"));
    EventoEntregaNoSatisfactoria evento =
        new EventoEntregaNoSatisfactoria(donante, entidad, "Destino cerrado");

    List<Notificacion> notificaciones = evento.generarNotificaciones();

    assertEquals(2, notificaciones.size());
    for (Notificacion n : notificaciones) {
      assertInstanceOf(SinPayload.class, n.getPayload());
      assertEquals(EstadoNotificacion.PENDIENTE, n.getEstado());
      assertTrue(n.getMensaje().contains("Destino cerrado"));
    }
  }

  @Test
  void eventoInicioRuta_generaNotificaciones_conInfoMapa() {
    entidad.agregarCorreo(new CorreoElectronico("entidad@test.com"));
    List<Notificable> destinatarios = List.of(donante, entidad);
    EventoInicioRuta evento =
        new EventoInicioRuta(destinatarios, "https://maps.example.com/ruta/1");

    List<Notificacion> notificaciones = evento.generarNotificaciones();

    assertEquals(2, notificaciones.size());
    for (Notificacion n : notificaciones) {
      assertInstanceOf(InfoMapa.class, n.getPayload());
      assertEquals(EstadoNotificacion.PENDIENTE, n.getEstado());
      InfoMapa mapa = (InfoMapa) n.getPayload();
      assertEquals("https://maps.example.com/ruta/1", mapa.url());
    }
  }
}
