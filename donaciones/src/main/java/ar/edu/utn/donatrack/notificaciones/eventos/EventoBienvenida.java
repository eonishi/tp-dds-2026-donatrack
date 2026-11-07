package ar.edu.utn.donatrack.notificaciones.eventos;

import ar.edu.utn.donatrack.notificaciones.DestinatariosNotificacion;
import ar.edu.utn.donatrack.notificaciones.EstadoNotificacion;
import ar.edu.utn.donatrack.notificaciones.Notificable;
import ar.edu.utn.donatrack.notificaciones.Notificacion;
import ar.edu.utn.donatrack.notificaciones.payloads.SinPayload;
import java.util.List;
import java.util.Objects;

public class EventoBienvenida extends EventoNotificable {
  private final Notificable titular;
  private final String enlaceIngreso;

  public EventoBienvenida(Notificable titular) {
    this(titular, "/login");
  }

  public EventoBienvenida(Notificable titular, String enlaceIngreso) {
    this.titular = Objects.requireNonNull(titular);
    this.enlaceIngreso = Objects.requireNonNull(enlaceIngreso);
  }

  @Override
  public List<Notificacion> generarNotificaciones() {
    return DestinatariosNotificacion.predeterminado(titular).stream()
        .map(
            medio ->
                new Notificacion(
                    titular,
                    medio,
                    "¡Bienvenido a DonaTrack! Gracias por formar parte del proyecto. "
                        + enlaceIngreso,
                    SinPayload.INSTANCE,
                    EstadoNotificacion.PENDIENTE))
        .toList();
  }
}
