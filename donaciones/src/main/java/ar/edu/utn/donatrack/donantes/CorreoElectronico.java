package ar.edu.utn.donatrack.donantes;

import ar.edu.utn.donatrack.notificaciones.Notificador;
import ar.edu.utn.donatrack.notificaciones.ResultadoEnvio;

public class CorreoElectronico extends MedioDeContacto {
  public CorreoElectronico(String contacto) {
    super(contacto);
  }

  @Override
  public ResultadoEnvio aceptar(Notificador notificador, String mensaje) {
    return notificador.enviar(this, mensaje);
  }
}
