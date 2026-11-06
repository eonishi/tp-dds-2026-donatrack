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
    ResultadoEnvio resultado = intentarEnvio(notificacion);
    if (!resultado.exitoso()) {
      LOGGER.warn(
          "Fallo de notificación canal={} destinatario={} detalle={}",
          notificacion.getDestinatario().getClass().getSimpleName(),
          enmascarar(notificacion.getDestinatario().getContacto()),
          resultado.detalle());
    }
    return notificacion;
  }

  private ResultadoEnvio intentarEnvio(Notificacion notificacion) {
    try {
      return notificador.enviarNotificacion(
          notificacion.getDestinatario(), notificacion.getMensaje());
    } catch (RuntimeException error) {
      return ResultadoEnvio.fallido(
          error.getMessage() == null ? "Error inesperado." : error.getMessage());
    }
  }

  private String enmascarar(String destinatario) {
    if (destinatario == null || destinatario.length() < 3) {
      return "***";
    }
    int arroba = destinatario.indexOf('@');
    if (arroba > 0) {
      return destinatario.substring(0, 1) + "***" + destinatario.substring(arroba);
    }
    return "***" + destinatario.substring(destinatario.length() - 2);
  }
}
