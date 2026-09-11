package ar.edu.utn.donatrack.notificaciones.payloads;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Comprobante de una entrega exitosa: cuándo se entregó y con qué camión.
 *
 * @param fechaEntrega fecha y hora de la entrega (la del historial de la transición a {@code
 *     ENTREGADA})
 * @param idCamion patente del camión que realizó la entrega (no nula)
 */
public record ComprobanteEntrega(LocalDateTime fechaEntrega, String idCamion)
    implements NotificacionPayload {

  public ComprobanteEntrega {
    Objects.requireNonNull(fechaEntrega, "fechaEntrega no debe ser null");
    Objects.requireNonNull(idCamion, "idCamion no debe ser null");
  }
}
