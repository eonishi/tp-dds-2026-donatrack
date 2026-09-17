package ar.edu.utn.donatrack.donantes;

import ar.edu.utn.donatrack.notificaciones.Notificador;
import javax.persistence.Entity;

// El identificador se hereda de MedioDeContacto; volver a declararlo aquí duplicaba el id mapeado.
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
