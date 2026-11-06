package ar.edu.utn.donatrack.notificaciones;

import java.util.List;

/** Destinatario de notificaciones: sabe por qué medios de contacto notificarse. */
public interface Notificable {
  /**
   * Envía el mensaje por los medios de contacto que este destinatario elige.
   *
   * @param mensaje texto a enviar
   * @param canales gateways disponibles para enviar
   * @return un envío por cada medio usado
   */
  List<Envio> notificar(String mensaje, NotificadorPorCanal canales);
}
