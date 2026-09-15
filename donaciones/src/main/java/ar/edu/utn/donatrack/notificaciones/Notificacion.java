package ar.edu.utn.donatrack.notificaciones;

import ar.edu.utn.donatrack.donantes.MedioDeContacto;
import ar.edu.utn.donatrack.notificaciones.payloads.NotificacionPayload;
import java.time.LocalDateTime;
import java.util.Objects;

public class Notificacion {
  private final MedioDeContacto destinatario;
  private final String mensaje;
  private final NotificacionPayload payload;
  private final LocalDateTime fechaCreacion;
  private EstadoNotificacion estado;
  private int intentos;
  private String ultimoError;

  public Notificacion(
      MedioDeContacto destinatario,
      String mensaje,
      NotificacionPayload payload,
      EstadoNotificacion estado) {
    this.destinatario = Objects.requireNonNull(destinatario);
    this.mensaje = Objects.requireNonNull(mensaje);
    this.payload = Objects.requireNonNull(payload);
    this.fechaCreacion = LocalDateTime.now();
    this.estado = Objects.requireNonNull(estado);
  }

  public MedioDeContacto getDestinatario() {
    return destinatario;
  }

  public String getMensaje() {
    return mensaje;
  }

  public NotificacionPayload getPayload() {
    return payload;
  }

  public LocalDateTime getFechaCreacion() {
    return fechaCreacion;
  }

  public EstadoNotificacion getEstado() {
    return estado;
  }

  public void marcarEnviada() {
    this.estado = EstadoNotificacion.ENVIADA;
    this.ultimoError = null;
  }

  public void marcarFallida(String detalle) {
    this.estado = EstadoNotificacion.FALLIDA;
    this.ultimoError = Objects.requireNonNull(detalle);
  }

  public void registrarIntento() {
    this.intentos++;
  }

  public int getIntentos() {
    return intentos;
  }

  public String getUltimoError() {
    return ultimoError;
  }
}
