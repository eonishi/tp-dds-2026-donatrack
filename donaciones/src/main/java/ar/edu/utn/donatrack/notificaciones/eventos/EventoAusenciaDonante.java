package ar.edu.utn.donatrack.notificaciones.eventos;

import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.notificaciones.EstadoNotificacion;
import ar.edu.utn.donatrack.notificaciones.DestinatariosNotificacion;
import ar.edu.utn.donatrack.notificaciones.Notificacion;
import ar.edu.utn.donatrack.notificaciones.payloads.SinPayload;
import java.util.List;
import java.util.Objects;

public class EventoAusenciaDonante extends EventoNotificable {
  private final Donante donante;

  public EventoAusenciaDonante(Donante donante) {
    this.donante = Objects.requireNonNull(donante);
  }

  @Override
  public List<Notificacion> generarNotificaciones() {
    return DestinatariosNotificacion.predeterminado(donante).stream()
        .map(
            medio ->
                new Notificacion(
                    medio,
                    "Hace más de 20 días que no recibimos una donación tuya. "
                        + "¿Te animas a colaborar nuevamente?",
                    SinPayload.INSTANCE,
                    EstadoNotificacion.PENDIENTE))
        .toList();
  }
}
