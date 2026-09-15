package ar.edu.utn.donatrack.notificaciones;

import java.util.List;
import java.util.Objects;

public class ServicioNotificaciones {
  private final Notificador notificador;
  private final NotificacionesRepository repository;
  private final int maxIntentos;

  public ServicioNotificaciones(
      Notificador notificador, NotificacionesRepository repository, int maxIntentos) {
    this.notificador = Objects.requireNonNull(notificador);
    this.repository = Objects.requireNonNull(repository);
    if (maxIntentos < 1) {
      throw new IllegalArgumentException("Debe configurarse al menos un intento.");
    }
    this.maxIntentos = maxIntentos;
  }

  public List<Notificacion> enviar(List<Notificacion> notificaciones) {
    notificaciones.forEach(this::enviar);
    return List.copyOf(notificaciones);
  }

  public Notificacion enviar(Notificacion notificacion) {
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
    return notificacion;
  }

  private ResultadoEnvio intentarEnvio(Notificacion notificacion) {
    try {
      return notificador.enviarNotificacion(
          notificacion.getDestinatario(), notificacion.getMensaje());
    } catch (RuntimeException error) {
      return ResultadoEnvio.fallido(error.getMessage() == null ? "Error inesperado." : error.getMessage());
    }
  }
}
