package ar.edu.utn.donatrack.notificaciones;

import ar.edu.utn.donatrack.donantes.MedioDeContacto;
import ar.edu.utn.donatrack.notificaciones.payloads.NotificacionPayload;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

public class Notificacion {
  // A quién pertenece el aviso (donante o entidad), para su bandeja en la web. Puede faltar: los
  // avisos a la administración van a un correo suelto.
  private final Notificable titular;
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
    this(null, destinatario, mensaje, payload, estado);
  }

  public Notificacion(
      Notificable titular,
      MedioDeContacto destinatario,
      String mensaje,
      NotificacionPayload payload,
      EstadoNotificacion estado) {
    this.titular = titular;
    this.destinatario = Objects.requireNonNull(destinatario);
    this.mensaje = Objects.requireNonNull(mensaje);
    this.payload = Objects.requireNonNull(payload);
    this.fechaCreacion = LocalDateTime.now();
    this.estado = Objects.requireNonNull(estado);
  }

  public Optional<Notificable> getTitular() {
    return Optional.ofNullable(titular);
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
