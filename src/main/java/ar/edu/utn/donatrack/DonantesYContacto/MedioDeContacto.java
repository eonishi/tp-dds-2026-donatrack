package ar.edu.utn.donatrack.DonantesYContacto;

public abstract class MedioDeContacto {
  private String contacto;

  //Constructor

  public MedioDeContacto(String contacto) {
    this.contacto = contacto;
  }

  //Método abstracto
  public abstract void enviarMensaje (String mensaje);
}
