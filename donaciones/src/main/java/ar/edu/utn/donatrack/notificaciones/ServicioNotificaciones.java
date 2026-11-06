package ar.edu.utn.donatrack.notificaciones;

import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServicioNotificaciones {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServicioNotificaciones.class);
  private final Notificador notificador;
  private final NotificacionesRepository repository;
  private final int maxIntentos;
  private final BandejaNotificaciones bandeja;

  public ServicioNotificaciones(
      Notificador notificador, NotificacionesRepository repository, int maxIntentos) {
    this(notificador, repository, maxIntentos, BandejaNotificaciones.NINGUNA);
  }

  public ServicioNotificaciones(
      Notificador notificador,
      NotificacionesRepository repository,
      int maxIntentos,
      BandejaNotificaciones bandeja) {
    this.bandeja = Objects.requireNonNull(bandeja);
    this.notificador = Objects.requireNonNull(notificador);
    this.repository = Objects.requireNonNull(repository);
    if (maxIntentos < 1) {
      throw new IllegalArgumentException("Debe configurarse al menos un intento.");
    }
    this.maxIntentos = maxIntentos;
  }

  public List<Notificacion> enviar(List<Notificacion> notificaciones) {
    notificaciones.forEach(this::entregar);
    registrarEnBandeja(notificaciones);
    return List.copyOf(notificaciones);
  }

  public Notificacion enviar(Notificacion notificacion) {
    entregar(notificacion);
    registrarEnBandeja(List.of(notificacion));
    return notificacion;
  }

  // La bandeja web no debe impedir el envío: si falla, queda en el log.
  private void registrarEnBandeja(List<Notificacion> notificaciones) {
    try {
      bandeja.registrar(notificaciones);
    } catch (RuntimeException error) {
      LOGGER.warn("No se pudieron guardar los avisos en la bandeja web", error);
    }
  }

  private Notificacion entregar(Notificacion notificacion) {
    repository.save(notificacion);
    for (int intento = 0; intento < maxIntentos; intento++) {
      notificacion.registrarIntento();
      ResultadoEnvio resultado = intentarEnvio(notificacion);
      if (resultado.exitoso()) {
        notificacion.marcarEnviada();
        return notificacion;
      }
      notificacion.marcarFallida(resultado.detalle());
    }
    LOGGER.warn(
        "Fallo de notificación canal={} destinatario={} intentos={} ultimoError={}",
        notificacion.getDestinatario().getClass().getSimpleName(),
        enmascarar(notificacion.getDestinatario().getContacto()),
        notificacion.getIntentos(),
        notificacion.getUltimoError());
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
