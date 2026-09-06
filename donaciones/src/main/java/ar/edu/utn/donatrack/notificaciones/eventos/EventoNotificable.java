package ar.edu.utn.donatrack.notificaciones.eventos;

import ar.edu.utn.donatrack.notificaciones.Notificacion;
import java.util.List;

public abstract class EventoNotificable {
  public abstract List<Notificacion> generarNotificaciones();
}
