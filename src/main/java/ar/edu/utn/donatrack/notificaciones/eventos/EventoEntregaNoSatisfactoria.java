package ar.edu.utn.donatrack.notificaciones.eventos;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.notificaciones.EstadoNotificacion;
import ar.edu.utn.donatrack.notificaciones.Notificacion;
import ar.edu.utn.donatrack.notificaciones.payloads.SinPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EventoEntregaNoSatisfactoria extends EventoNotificable {
  private final Donante donante;
  private final EntidadBeneficiaria entidad;
  private final String motivo;

  public EventoEntregaNoSatisfactoria(Donante donante, EntidadBeneficiaria entidad, String motivo) {
    this.donante = Objects.requireNonNull(donante);
    this.entidad = Objects.requireNonNull(entidad);
    this.motivo = Objects.requireNonNull(motivo);
  }

  @Override
  public List<Notificacion> generarNotificaciones() {
    List<Notificacion> notificaciones = new ArrayList<>();
    String mensajeDonante =
        "La entrega a " + entidad.getRazonSocial() + " no pudo completarse: " + motivo;
    for (var medio : donante.getMediosDeContacto()) {
      notificaciones.add(
          new Notificacion(
              medio, mensajeDonante, SinPayload.INSTANCE, EstadoNotificacion.PENDIENTE));
    }
    String mensajeEntidad = "La entrega no pudo completarse: " + motivo;
    for (var medio : entidad.getMediosDeContacto()) {
      notificaciones.add(
          new Notificacion(
              medio, mensajeEntidad, SinPayload.INSTANCE, EstadoNotificacion.PENDIENTE));
    }
    return notificaciones;
  }
}
