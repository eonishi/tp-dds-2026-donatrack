package ar.edu.utn.donatrack.notificaciones.eventos;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.logistica.Camion;
import ar.edu.utn.donatrack.notificaciones.EstadoNotificacion;
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
  private final Camion camion;

  public EventoEntregaExitosa(
      Donante donante, EntidadBeneficiaria entidad, LocalDateTime fechaEntrega, Camion camion) {
    this.donante = Objects.requireNonNull(donante);
    this.entidad = Objects.requireNonNull(entidad);
    this.fechaEntrega = Objects.requireNonNull(fechaEntrega);
    this.camion = Objects.requireNonNull(camion);
  }

  @Override
  public List<Notificacion> generarNotificaciones() {
    ComprobanteEntrega comprobante = new ComprobanteEntrega(fechaEntrega, camion);
    List<Notificacion> notificaciones = new ArrayList<>();
    String mensajeDonante =
        "Tu donación fue entregada exitosamente a " + entidad.getRazonSocial() + ".";
    for (var medio : donante.getMediosDeContacto()) {
      notificaciones.add(
          new Notificacion(medio, mensajeDonante, comprobante, EstadoNotificacion.PENDIENTE));
    }
    String mensajeEntidad = "Has recibido una donación exitosamente.";
    for (var medio : entidad.getMediosDeContacto()) {
      notificaciones.add(
          new Notificacion(medio, mensajeEntidad, comprobante, EstadoNotificacion.PENDIENTE));
    }
    return notificaciones;
  }
}
