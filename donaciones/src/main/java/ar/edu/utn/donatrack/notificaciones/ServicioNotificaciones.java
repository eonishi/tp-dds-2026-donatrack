package ar.edu.utn.donatrack.notificaciones;

import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServicioNotificaciones {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServicioNotificaciones.class);
  private final NotificadorPorCanal notificador;

  public ServicioNotificaciones(NotificadorPorCanal notificador) {
    this.notificador = Objects.requireNonNull(notificador);
  }

  public List<Notificacion> enviar(List<Notificacion> notificaciones) {
    notificaciones.forEach(this::enviar);
    return List.copyOf(notificaciones);
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
