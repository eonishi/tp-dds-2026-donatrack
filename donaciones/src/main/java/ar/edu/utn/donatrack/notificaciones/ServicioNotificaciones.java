package ar.edu.utn.donatrack.notificaciones;

import ar.edu.utn.donatrack.notificaciones.eventos.EventoNotificable;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServicioNotificaciones implements PublicadorDeEventos {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServicioNotificaciones.class);
  private final NotificadorPorCanal notificador;

  public ServicioNotificaciones(NotificadorPorCanal notificador) {
    this.notificador = Objects.requireNonNull(notificador);
  }

  /**
   * Publica un evento en el momento en que ocurre: envía cada notificación que genera.
   *
   * <p>Es el único punto por el que salen las notificaciones de un evento; un fallo de envío se
   * registra y no interrumpe al que publica.
   *
   * @param evento hecho ocurrido
   */
  @Override
  public void publicar(EventoNotificable evento) {
    enviar(evento.generarNotificaciones());
  }

  /**
   * Crea un publicador que retiene los eventos hasta que el caso de uso se confirma.
   *
   * @return un publicador nuevo, sin eventos pendientes
   */
  public PublicadorDiferido publicadorDiferido() {
    return new PublicadorDiferido(this);
  }

  private void enviar(List<Notificacion> notificaciones) {
    notificaciones.forEach(this::enviar);
  }

  public Notificacion enviar(Notificacion notificacion) {
    String destinatario = notificacion.getDestinatario().getClass().getSimpleName();
    try {
      for (Envio envio : notificacion.enviarCon(notificador)) {
        if (!envio.resultado().exitoso()) {
          LOGGER.warn(
              "Fallo de notificación destinatario={} canal={} detalle={}",
              destinatario,
              envio.canal(),
              envio.resultado().detalle());
        }
      }
    } catch (RuntimeException error) {
      LOGGER.warn(
          "Fallo de notificación destinatario={} detalle={}", destinatario, error.getMessage());
    }
    return notificacion;
  }
}
