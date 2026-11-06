package ar.edu.utn.donatrack.notificaciones;

import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.MedioDeContacto;
import java.util.List;

public final class DestinatariosNotificacion {
  private DestinatariosNotificacion() {}

  public static List<MedioDeContacto> predeterminado(Notificable notificable) {
    if (notificable instanceof Donante donante) {
      return List.of(donante.getMedioContactoPredeterminado());
    }
    return notificable.getMediosDeContacto();
  }
}
