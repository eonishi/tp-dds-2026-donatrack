package ar.edu.utn.donatrack.notificaciones;

import ar.edu.utn.donatrack.notificaciones.payloads.NotificacionPayload;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Notificacion {
  private final Notificable destinatario;
  private final String mensaje;
  private final NotificacionPayload payload;
  private final LocalDateTime fechaCreacion;
  private final List<Envio> envios = new ArrayList<>();

  public Notificacion(Notificable destinatario, String mensaje, NotificacionPayload payload) {
    this.destinatario = Objects.requireNonNull(destinatario);
    this.mensaje = Objects.requireNonNull(mensaje);
    this.payload = Objects.requireNonNull(payload);
    this.fechaCreacion = LocalDateTime.now();
  }

  /**
   * Delega el envío en el destinatario y registra por qué canales salió.
   *
   * @param canales gateways disponibles para enviar
   * @return los envíos realizados en este intento
   */
  public List<Envio> enviarCon(NotificadorPorCanal canales) {
    List<Envio> realizados = destinatario.notificar(mensaje, canales);
    envios.addAll(realizados);
    return realizados;
  }

  public Notificable getDestinatario() {
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

  public List<Envio> getEnvios() {
    return Collections.unmodifiableList(envios);
  }
}
