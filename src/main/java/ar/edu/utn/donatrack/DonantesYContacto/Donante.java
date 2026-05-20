package ar.edu.utn.donatrack.DonantesYContacto;

public class Donante {
  private static long id;
  public MedioDeContacto medioDeContacto;

  public void setMedioDeContacto(MedioDeContacto medioDeContacto) {
    this.medioDeContacto = medioDeContacto;
  }

  public static void setId(long id) {
    Donante.id = id;
  }
}
