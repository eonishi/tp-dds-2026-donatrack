package ar.edu.utn.donatrack.notificaciones.eventos;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.notificaciones.Notificacion;
import ar.edu.utn.donatrack.notificaciones.payloads.ComprobanteEntrega;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Evento de entrega exitosa: notifica al donante y a la entidad beneficiaria con un {@link
 * ComprobanteEntrega} que incluye la fecha de entrega y la patente del camión.
 */
public class EventoEntregaExitosa extends EventoNotificable {
  private static final DateTimeFormatter FORMATO_FECHA_ENTREGA =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  private final Donante donante;
  private final EntidadBeneficiaria entidad;
  private final LocalDateTime fechaEntrega;
  private final String idCamion;

  /**
   * Crea el evento de entrega exitosa.
   *
   * <p>La patente llega registrada desde el inicio del traslado (PATCH de transición a {@code
   * EN_TRASLADO}); no se vuelve a pedir al confirmar la entrega.
   *
   * @param donante donante de la donación entregada
   * @param entidad entidad beneficiaria que recibió la donación
   * @param fechaEntrega fecha y hora de la entrega (la del historial de la transición a {@code
   *     ENTREGADA})
   * @param idCamion patente del camión que realizó la entrega (no nula)
   * @throws NullPointerException si {@code idCamion} es {@code null}
   */
  public EventoEntregaExitosa(
      Donante donante, EntidadBeneficiaria entidad, LocalDateTime fechaEntrega, String idCamion) {
    this.donante = Objects.requireNonNull(donante);
    this.entidad = Objects.requireNonNull(entidad);
    this.fechaEntrega = Objects.requireNonNull(fechaEntrega);
    this.idCamion = Objects.requireNonNull(idCamion, "idCamion no debe ser null");
  }

  @Override
  public List<Notificacion> generarNotificaciones() {
    ComprobanteEntrega comprobante = new ComprobanteEntrega(fechaEntrega, idCamion);
    List<Notificacion> notificaciones = new ArrayList<>();
    String detalleEntrega =
        " Fecha y hora: "
            + fechaEntrega.format(FORMATO_FECHA_ENTREGA)
            + ". Camión: "
            + idCamion
            + ".";
    String mensajeDonante =
        "Tu donación fue entregada exitosamente a "
            + entidad.getRazonSocial()
            + "."
            + detalleEntrega;
    notificaciones.add(new Notificacion(donante, mensajeDonante, comprobante));
    String mensajeEntidad = "Has recibido una donación exitosamente." + detalleEntrega;
    notificaciones.add(new Notificacion(entidad, mensajeEntidad, comprobante));
    return notificaciones;
  }
}
