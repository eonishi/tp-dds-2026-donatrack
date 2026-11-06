package ar.edu.utn.donatrack.donantes;

import ar.edu.utn.donatrack.notificaciones.Envio;
import ar.edu.utn.donatrack.notificaciones.NotificadorPorCanal;
import javax.persistence.Entity;

@Entity
public class Sms extends MedioDeContacto {

  protected Sms() {}

  public Sms(String contacto) {
    super(contacto);
  }

  @Override
  public Envio enviar(String mensaje, NotificadorPorCanal canales) {
    return canales.enviarPorSms(getContacto(), mensaje);
  }
}
