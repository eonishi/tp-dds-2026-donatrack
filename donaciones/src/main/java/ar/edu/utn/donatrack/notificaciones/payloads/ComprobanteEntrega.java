package ar.edu.utn.donatrack.notificaciones.payloads;

import java.time.LocalDateTime;

public record ComprobanteEntrega(LocalDateTime fecha) implements NotificacionPayload {
  /*
   * ── LÓGICA FALTANTE — microservicio de LOGÍSTICA ──────────────────────
   * El camión que realizó la entrega vive en el microservicio de logística.
   * No migrada: al integrar, el comprobante debe incluir los datos del
   * camión provistos por el microservicio de logística.
   *
   *   public record ComprobanteEntrega(LocalDateTime fecha, Camion camion)
   *       implements NotificacionPayload {}
   */
}
