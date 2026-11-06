package ar.edu.utn.donatrack.donantes;

import ar.edu.utn.donatrack.notificaciones.NotificadorPorCanal;
import javax.persistence.Entity;

@Entity
public class Whatsapp extends MedioDeContacto {

  protected Whatsapp() {}

  public Whatsapp(String contacto) {
    super(contacto);
  }

  @Override
  public void enviar(String mensaje, NotificadorPorCanal canales) {
    canales.enviarPorWhatsapp(getContacto(), mensaje);
  }
}
