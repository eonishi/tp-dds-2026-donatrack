package ar.edu.utn.donatrack.notificaciones.eventos;

import ar.edu.utn.donatrack.notificaciones.Notificable;
import ar.edu.utn.donatrack.notificaciones.Notificacion;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Aviso de que la donación salió hacia la entidad, con el enlace para seguir al camión.
 *
 * <p>El dominio solo conoce la patente: la base del mapa de tracking sale de la configuración
 * ({@code DONATRACK_TRACKING_URL}, o la propiedad {@code donatrack.tracking.url} en tests) y, si no
 * está definida, se usa {@value #URL_TRACKING_POR_DEFECTO}.
 */
public class EventoInicioRuta implements EventoNotificable {
  static final String URL_TRACKING_POR_DEFECTO = "https://mapa.donatrack.org/tracking";

  private final List<Notificable> destinatarios;
  private final String idCamion;

  /**
   * @param destinatarios quienes reciben el aviso
   * @param idCamion patente del camión que hace el traslado (no nula)
   */
  public EventoInicioRuta(List<Notificable> destinatarios, String idCamion) {
    this.destinatarios = List.copyOf(destinatarios);
    this.idCamion = Objects.requireNonNull(idCamion);
  }

  @Override
  public List<Notificacion> generarNotificaciones() {
    String urlMapa = baseTracking() + "/" + idCamion;
    List<Notificacion> notificaciones = new ArrayList<>();
    for (var notificable : destinatarios) {
      notificaciones.add(
          new Notificacion(
              notificable,
              "La ruta de entrega ha comenzado. Seguí el recorrido en el mapa: " + urlMapa));
    }
    return notificaciones;
  }

  private static String baseTracking() {
    String base =
        System.getProperty("donatrack.tracking.url", System.getenv("DONATRACK_TRACKING_URL"));
    if (base == null || base.isBlank()) {
      return URL_TRACKING_POR_DEFECTO;
    }
    String recortada = base.strip();
    return recortada.endsWith("/") ? recortada.substring(0, recortada.length() - 1) : recortada;
  }
}
