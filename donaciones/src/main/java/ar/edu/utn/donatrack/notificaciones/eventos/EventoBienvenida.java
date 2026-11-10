package ar.edu.utn.donatrack.notificaciones.eventos;

import ar.edu.utn.donatrack.notificaciones.Notificable;
import ar.edu.utn.donatrack.notificaciones.Notificacion;
import java.util.List;
import java.util.Objects;

public class EventoBienvenida implements EventoNotificable {
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
    return List.of(
        new Notificacion(
            titular,
            "¡Bienvenido a DonaTrack! Gracias por formar parte del proyecto. " + enlaceIngreso));
  }
}
