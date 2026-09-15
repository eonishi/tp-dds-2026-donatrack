package ar.edu.utn.donatrack.notificaciones.eventos;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.notificaciones.EstadoNotificacion;
import ar.edu.utn.donatrack.notificaciones.DestinatariosNotificacion;
import ar.edu.utn.donatrack.notificaciones.Notificacion;
import ar.edu.utn.donatrack.notificaciones.payloads.ComprobanteEntrega;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EventoEntregaExitosa extends EventoNotificable {
  private final Donante donante;
  private final EntidadBeneficiaria entidad;
  private final LocalDateTime fechaEntrega;

  /*
   * ── LÓGICA FALTANTE — microservicio de LOGÍSTICA ──────────────────────
   * El camión que realizó la entrega lo conoce el microservicio de
   * logística. No migrada: al integrar, el evento de entrega exitosa debe
   * recibir el camión desde el microservicio de logística.
   *
   *   private final Camion camion;
   */

  public EventoEntregaExitosa(
      Donante donante, EntidadBeneficiaria entidad, LocalDateTime fechaEntrega) {
    this.donante = Objects.requireNonNull(donante);
    this.entidad = Objects.requireNonNull(entidad);
    this.fechaEntrega = Objects.requireNonNull(fechaEntrega);
  }

  @Override
  public List<Notificacion> generarNotificaciones() {
    // ── LÓGICA FALTANTE — microservicio de LOGÍSTICA ──────────────────────
    // El comprobante original incluía el camión (ver ComprobanteEntrega).
    // No migrada: el camión llegará por integración con logística.
    ComprobanteEntrega comprobante = new ComprobanteEntrega(fechaEntrega);
    List<Notificacion> notificaciones = new ArrayList<>();
    String mensajeDonante =
        "Tu donación fue entregada exitosamente a " + entidad.getRazonSocial() + ".";
    for (var medio : DestinatariosNotificacion.predeterminado(donante)) {
      notificaciones.add(
          new Notificacion(medio, mensajeDonante, comprobante, EstadoNotificacion.PENDIENTE));
    }
    String mensajeEntidad = "Has recibido una donación exitosamente.";
    for (var medio : DestinatariosNotificacion.predeterminado(entidad)) {
      notificaciones.add(
          new Notificacion(medio, mensajeEntidad, comprobante, EstadoNotificacion.PENDIENTE));
    }
    return notificaciones;
  }
}
