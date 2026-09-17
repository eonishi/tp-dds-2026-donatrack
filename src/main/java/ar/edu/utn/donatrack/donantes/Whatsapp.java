package ar.edu.utn.donatrack.donantes;

import ar.edu.utn.donatrack.notificaciones.Notificador;
import javax.persistence.Entity;

// Ver Sms: Whatsapp debe ser una entidad mapeada para sobrevivir a la lista polimórfica
// mediosDeContacto.
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
