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

  public Notificacion(MedioDeContacto destinatario, String mensaje, NotificacionPayload payload) {
    this.destinatario = Objects.requireNonNull(destinatario);
    this.mensaje = Objects.requireNonNull(mensaje);
    this.payload = Objects.requireNonNull(payload);
    this.fechaCreacion = LocalDateTime.now();
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
}
