package ar.edu.utn.donatrack.DonantesYContacto;

public class Donante {
  private static long id;
  public MedioDeContacto medioDeContacto;

  //Constructor
  public Donante(long id) {
    Donante.id = id;
  }

  //Métodos
  public void setMedioDeContacto(MedioDeContacto medioDeContacto) {
    this.medioDeContacto = medioDeContacto;
  }

  public void setMedioContactoPredterminado(MedioDeContacto medioDeContacto){
    this.medioDeContacto = medioDeContacto;
  }


}
