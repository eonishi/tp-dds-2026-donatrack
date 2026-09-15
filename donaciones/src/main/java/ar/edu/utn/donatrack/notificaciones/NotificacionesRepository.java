package ar.edu.utn.donatrack.notificaciones;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificacionesRepository {
  private final List<Notificacion> notificaciones = new ArrayList<>();

  public synchronized Notificacion save(Notificacion notificacion) {
    notificaciones.add(notificacion);
    return notificacion;
  }

  public synchronized List<Notificacion> getNotificaciones() {
    return Collections.unmodifiableList(new ArrayList<>(notificaciones));
  }
}
