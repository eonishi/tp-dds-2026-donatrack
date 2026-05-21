package ar.edu.utn.donatrack.DonantesYContacto;

public class Donante {
  public MedioDeContacto medioDeContacto;
  protected int numDocumento;
  protected TipoDocumento documento;

  //Constructor
  public Donante(MedioDeContacto medioDeContacto,int numDocumento, TipoDocumento documento) {
    this.medioDeContacto = medioDeContacto;
    this.numDocumento = numDocumento;
    this.documento = documento;

  }

  //Métodos
  public void setMedioDeContacto(MedioDeContacto medioDeContacto) {
    this.medioDeContacto = medioDeContacto;
  }

  public void setMedioContactoPredterminado(MedioDeContacto medioDeContacto){
    this.medioDeContacto = medioDeContacto;
  }

}
