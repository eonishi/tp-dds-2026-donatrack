package ar.edu.utn.donatrack.notificaciones;

import java.util.Objects;

/**
 * Aviso para un destinatario.
 *
 * @param destinatario quien recibe el aviso
 * @param mensaje texto a enviar
 */
public record Notificacion(Notificable destinatario, String mensaje) {
  public Notificacion {
    Objects.requireNonNull(destinatario);
    Objects.requireNonNull(mensaje);
  }

  /**
   * Delega el envío en el destinatario.
   *
   * @param canales gateways disponibles para enviar
   */
  public void enviarCon(NotificadorPorCanal canales) {
    destinatario.notificar(mensaje, canales);
  }
}
