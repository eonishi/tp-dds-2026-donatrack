package ar.edu.utn.donatrack.donantes;

import ar.edu.utn.donatrack.notificaciones.Notificador;
import javax.persistence.Entity;

// See Sms: Whatsapp must be a mapped entity to survive the polymorphic mediosDeContacto list.
@Entity
public class Whatsapp extends MedioDeContacto {

  protected Whatsapp() {}

  public Whatsapp(String contacto) {
    super(contacto);
  }

  @Override
  public void aceptar(Notificador notificador, String mensaje) {
    notificador.enviar(this, mensaje);
  }
}
