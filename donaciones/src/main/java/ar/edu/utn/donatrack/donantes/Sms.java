package ar.edu.utn.donatrack.donantes;

import ar.edu.utn.donatrack.notificaciones.Notificador;
import ar.edu.utn.donatrack.notificaciones.ResultadoEnvio;
import javax.persistence.Entity;

@Entity
public class Sms extends MedioDeContacto {

  protected Sms() {}

  public Sms(String contacto) {
    super(contacto);
  }

  @Override
  public ResultadoEnvio aceptar(Notificador notificador, String mensaje) {
    return notificador.enviar(this, mensaje);
  }
}
