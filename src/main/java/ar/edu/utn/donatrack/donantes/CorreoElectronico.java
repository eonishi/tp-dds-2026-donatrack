package ar.edu.utn.donatrack.donantes;

public class CorreoElectronico extends MedioDeContacto {
  public CorreoElectronico(String contacto) {
    super(contacto);
  }

  @Override
  public void enviarMensaje(String mensaje) {}
}
