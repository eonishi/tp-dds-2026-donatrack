package ar.edu.utn.donatrack.notificaciones.eventos;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.notificaciones.DestinatariosNotificacion;
import ar.edu.utn.donatrack.notificaciones.Notificacion;
import ar.edu.utn.donatrack.notificaciones.payloads.SinPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EventoAsignacion extends EventoNotificable {
  private final Donante donante;
  private final EntidadBeneficiaria entidad;

  public EventoAsignacion(Donante donante, EntidadBeneficiaria entidad) {
    this.donante = Objects.requireNonNull(donante);
    this.entidad = Objects.requireNonNull(entidad);
  }

  @Override
  public List<Notificacion> generarNotificaciones() {
    List<Notificacion> notificaciones = new ArrayList<>();
    String mensajeDonante = "Tu donación ha sido asignada a " + entidad.getRazonSocial() + ".";
    for (var medio : DestinatariosNotificacion.predeterminado(donante)) {
      notificaciones.add(new Notificacion(medio, mensajeDonante, SinPayload.INSTANCE));
    }
    String mensajeEntidad = "Se ha asignado una donación a tu entidad.";
    for (var medio : DestinatariosNotificacion.predeterminado(entidad)) {
      notificaciones.add(new Notificacion(medio, mensajeEntidad, SinPayload.INSTANCE));
    }
    return notificaciones;
  }
}
