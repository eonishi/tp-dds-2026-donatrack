package ar.edu.utn.donatrack.donantes;

import ar.edu.utn.donatrack.notificaciones.NotificadorPorCanal;
import javax.persistence.Entity;

@Entity
public class CorreoElectronico extends MedioDeContacto {
  protected CorreoElectronico() {}

  public CorreoElectronico(String contacto) {
    super(contacto);
  }

  @Override
  public void enviar(String mensaje, NotificadorPorCanal canales) {
    canales.enviarPorCorreo(getContacto(), mensaje);
  }
}
