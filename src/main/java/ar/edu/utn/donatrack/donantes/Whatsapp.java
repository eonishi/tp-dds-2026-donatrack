package ar.edu.utn.donatrack.donantes;

public class Whatsapp extends MedioDeContacto {

  // Constructor
  public Whatsapp(String contacto) {

    super(contacto);
  }

  // Método
  @Override
  public void enviarMensaje(String mensaje) {}
}
