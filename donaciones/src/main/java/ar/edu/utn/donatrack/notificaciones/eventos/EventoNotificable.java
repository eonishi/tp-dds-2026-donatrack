package ar.edu.utn.donatrack.notificaciones.eventos;

import ar.edu.utn.donatrack.notificaciones.Notificacion;
import java.util.List;

/**
 * Hecho del dominio que avisa a alguien.
 *
 * <p>Es interfaz porque {@link ar.edu.utn.donatrack.notificaciones.ServicioNotificaciones#publicar}
 * trata a todos los eventos por igual: solo necesita las notificaciones que cada uno genera.
 */
public interface EventoNotificable {
  /**
   * Arma las notificaciones que este evento dispara.
   *
   * @return una notificación por destinatario
   */
  List<Notificacion> generarNotificaciones();
}
