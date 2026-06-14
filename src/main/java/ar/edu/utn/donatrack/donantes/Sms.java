package ar.edu.utn.donatrack.donantes;

public class Sms extends MedioDeContacto {

  // Constructor
  public Sms(String contacto) {

    super(contacto);
  }

  // Método
  @Override
  public void enviarMensaje(String mensaje) {}
}
