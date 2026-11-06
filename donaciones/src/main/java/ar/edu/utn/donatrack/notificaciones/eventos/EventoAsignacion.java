package ar.edu.utn.donatrack.notificaciones.eventos;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.notificaciones.Notificacion;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EventoAsignacion implements EventoNotificable {
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
    notificaciones.add(new Notificacion(donante, mensajeDonante));
    String mensajeEntidad = "Se ha asignado una donación a tu entidad.";
    notificaciones.add(new Notificacion(entidad, mensajeEntidad));
    return notificaciones;
  }
}
