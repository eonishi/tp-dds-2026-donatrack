package ar.edu.utn.donatrack.notificaciones;

import ar.edu.utn.donatrack.donantes.MedioDeContacto;
import java.util.List;

public interface Notificable {
  List<MedioDeContacto> getMediosDeContacto();
}
