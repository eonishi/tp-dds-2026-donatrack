package ar.edu.utn.donatrack.notificaciones.eventos;

import ar.edu.utn.donatrack.notificaciones.DestinatariosNotificacion;
import ar.edu.utn.donatrack.notificaciones.EstadoNotificacion;
import ar.edu.utn.donatrack.notificaciones.Notificable;
import ar.edu.utn.donatrack.notificaciones.Notificacion;
import ar.edu.utn.donatrack.notificaciones.payloads.InfoMapa;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EventoInicioRuta extends EventoNotificable {
  private final List<Notificable> destinatarios;
  private final String urlMapa;

  public EventoInicioRuta(List<Notificable> destinatarios, String urlMapa) {
    this.destinatarios = List.copyOf(destinatarios);
    this.urlMapa = Objects.requireNonNull(urlMapa);
  }

  @Override
  public List<Notificacion> generarNotificaciones() {
    InfoMapa payload = new InfoMapa(urlMapa);
    List<Notificacion> notificaciones = new ArrayList<>();
    for (var notificable : destinatarios) {
      for (var medio : DestinatariosNotificacion.predeterminado(notificable)) {
        notificaciones.add(
            new Notificacion(
                medio,
                "La ruta de entrega ha comenzado. Seguí el recorrido en el mapa: " + urlMapa,
                payload,
                EstadoNotificacion.PENDIENTE));
      }
    }
    return notificaciones;
  }
}
