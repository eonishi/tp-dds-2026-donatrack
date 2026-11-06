package ar.edu.utn.donatrack.donantes;

import ar.edu.utn.donatrack.notificaciones.NotificadorPorCanal;
import javax.persistence.Entity;

@Entity
public class Sms extends MedioDeContacto {

  protected Sms() {}

  public Sms(String contacto) {
    super(contacto);
  }

  @Override
  public void enviar(String mensaje, NotificadorPorCanal canales) {
    canales.enviarPorSms(getContacto(), mensaje);
  }
}
