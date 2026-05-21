package ar.edu.utn.donatrack.DonantesYContacto;

public class PersonaHumana {
  private String nombre;
  private String apellido;
  private int edad;
  private int numDocumento;
  private TipoDocumento documento;
  private String genero;
  private String direccion;

  public PersonaHumana(String nombre, String apellido, int edad, int numDocumento, TipoDocumento documento, String genero, String direccion) {
    this.nombre = nombre;
    this.apellido = apellido;
    this.edad = edad;
    this.numDocumento = numDocumento;
    this.documento = documento;
    this.genero = genero;
    this.direccion = direccion;
  }
}
