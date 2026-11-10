package ar.edu.utn.donatrack.notificaciones;

import ar.edu.utn.donatrack.notificaciones.eventos.EventoNotificable;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServicioNotificaciones implements PublicadorDeEventos {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServicioNotificaciones.class);

  private final NotificadorPorCanal notificador;
  private final BandejaNotificaciones bandeja;

  public ServicioNotificaciones(NotificadorPorCanal notificador) {
    this(notificador, BandejaNotificaciones.NINGUNA);
  }

  public ServicioNotificaciones(NotificadorPorCanal notificador, BandejaNotificaciones bandeja) {
    this.notificador = Objects.requireNonNull(notificador);
    this.bandeja = Objects.requireNonNull(bandeja);
  }

  /**
   * Publica un evento en el momento en que ocurre: envía cada notificación que genera y la deja en
   * la bandeja web de su destinatario.
   *
   * <p>Es el único punto por el que salen las notificaciones de un evento; un fallo de envío se
   * registra y no interrumpe al que publica.
   *
   * @param evento hecho ocurrido
   */
  @Override
  public void publicar(EventoNotificable evento) {
    List<Notificacion> notificaciones = evento.generarNotificaciones();
    notificaciones.forEach(this::enviar);
    registrarEnBandeja(notificaciones);
  }

  /**
   * Crea un publicador que retiene los eventos hasta que el caso de uso se confirma.
   *
   * @return un publicador nuevo, sin eventos pendientes
   */
  public PublicadorDiferido publicadorDiferido() {
    return new PublicadorDiferido(this);
  }

  /**
   * Envía una notificación. Los fallos de los gateways ya los registra el notificador; acá se
   * contienen los errores del destinatario (por ejemplo, datos de contacto incompletos).
   *
   * @param notificacion aviso a enviar
   */
  public void enviar(Notificacion notificacion) {
    try {
      notificacion.enviarCon(notificador);
    } catch (RuntimeException error) {
      LOGGER.warn(
          "Fallo de notificación destinatario={} detalle={}",
          notificacion.destinatario().getClass().getSimpleName(),
          error.getMessage());
    }
  }

  // La bandeja web no debe impedir el envío: si falla, queda en el log.
  private void registrarEnBandeja(List<Notificacion> notificaciones) {
    try {
      bandeja.registrar(notificaciones);
    } catch (RuntimeException error) {
      LOGGER.warn("No se pudieron guardar los avisos en la bandeja web", error);
    }
  }
}
