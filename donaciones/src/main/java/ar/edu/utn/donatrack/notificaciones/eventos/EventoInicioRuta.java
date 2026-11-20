package ar.edu.utn.donatrack.notificaciones.eventos;

import ar.edu.utn.donatrack.notificaciones.Notificable;
import ar.edu.utn.donatrack.notificaciones.Notificacion;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Aviso de que la donación salió hacia la entidad, con el enlace para seguir al camión.
 *
 * <p>La URL pública se configura mediante {@code DONATRACK_WEB_ORIGIN}.
 */
public class EventoInicioRuta implements EventoNotificable {
  private final List<Notificable> destinatarios;

  /**
   * @param destinatarios quienes reciben el aviso
   * @param idCamion patente del camión que hace el traslado (no nula)
   */
  public EventoInicioRuta(List<Notificable> destinatarios, String idCamion) {
    this.destinatarios = List.copyOf(destinatarios);
    Objects.requireNonNull(idCamion);
  }

  @Override
  public List<Notificacion> generarNotificaciones() {
    List<Notificacion> notificaciones = new ArrayList<>();
    for (var notificable : destinatarios) {
      String ruta = notificable instanceof Donante ? "/donante/seguimiento"
          : notificable instanceof EntidadBeneficiaria ? "/entidad/seguimiento" : null;
      if (ruta == null) {
        throw new IllegalArgumentException("Destinatario de seguimiento no soportado");
      }
      String urlMapa = baseTracking() + ruta;
      notificaciones.add(
          new Notificacion(
              notificable,
              "La ruta de entrega ha comenzado. Seguí el recorrido en el mapa: " + urlMapa));
    }
    return notificaciones;
  }

  private static String baseTracking() {
    String base =
        System.getProperty("donatrack.web.origin", System.getenv("DONATRACK_WEB_ORIGIN"));
    if (base == null || base.isBlank()) {
      return "http://localhost:7070";
    }
    String recortada = base.strip();
    return recortada.endsWith("/") ? recortada.substring(0, recortada.length() - 1) : recortada;
  }
}
