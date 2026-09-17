package ar.edu.utn.donatrack.donantes;

import ar.edu.utn.donatrack.notificaciones.Notificador;
import javax.persistence.Entity;

// Sms is persisted as part of the MedioDeContacto JOINED hierarchy: a donor stores a polymorphic
// list of contact means, so every concrete mean must be a mapped entity of that hierarchy.
@Entity
public class Sms extends MedioDeContacto {

  protected Sms() {}

  public Sms(String contacto) {
    super(contacto);
  }

  @Override
  public void aceptar(Notificador notificador, String mensaje) {
    notificador.enviar(this, mensaje);
  }
}
