package ar.edu.utn.donatrack.donantes;

import ar.edu.utn.donatrack.notificaciones.Notificador;
import javax.persistence.Entity;

// The identifier is inherited from MedioDeContacto; redeclaring it here duplicated the mapped id.
@Entity
public class CorreoElectronico extends MedioDeContacto {

  protected CorreoElectronico() {}

  public CorreoElectronico(String contacto) {
    super(contacto);
  }

  @Override
  public void aceptar(Notificador notificador, String mensaje) {
    notificador.enviar(this, mensaje);
  }
}
