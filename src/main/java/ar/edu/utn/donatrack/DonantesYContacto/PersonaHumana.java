package ar.edu.utn.donatrack.DonantesYContacto;

public class PersonaHumana extends Donante{
  private String nombre;
  private String apellido;
  private int edad;
  private String genero;
  private String direccion;

  public PersonaHumana(MedioDeContacto medioDeContacto, String nombre, String apellido, int edad, int numDocumento, TipoDocumento documento, String genero, String direccion) {
    super(medioDeContacto, numDocumento, documento);
    this.nombre = nombre;
    this.apellido = apellido;
    this.edad = edad;
    this.genero = genero;
    this.direccion = direccion;


  }
}
