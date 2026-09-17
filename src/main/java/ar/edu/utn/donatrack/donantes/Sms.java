package ar.edu.utn.donatrack.donantes;

import ar.edu.utn.donatrack.notificaciones.Notificador;
import javax.persistence.Entity;

// Sms se persiste como parte de la jerarquía JOINED de MedioDeContacto: un donante almacena una
// lista polimórfica de medios de contacto, así que cada medio concreto debe ser una entidad mapeada
// de esa jerarquía.
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
