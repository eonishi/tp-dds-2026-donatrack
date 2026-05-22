package ar.edu.utn.donatrack.DonantesYContacto;

public class PersonaHumana extends Donante {
  private final String nombre;
  private final String apellido;
  private Integer edad;
  private String genero; // Quizas un enum (?)
  private String direccion;

  public PersonaHumana(
      CorreoElectronico medioDeContacto,
      String nombre,
      String apellido,
      Integer edad,
      String numDocumento,
      TipoDocumento documento,
      String genero,
      String direccion
  ) {
    super(medioDeContacto, numDocumento, documento);
    this.nombre = nombre;
    this.apellido = apellido;
    this.edad = edad;
    this.genero = genero;
    this.direccion = direccion;
  }


  public String getNombre() { return nombre; }
  public String getApellido() { return apellido; }
  public int getEdad() { return edad; }

  public void setGenero(String genero) { this.genero = genero; }
  public void setEdad(int edad){ this.edad = edad; }
  public void setDireccion(String direccion){ this.direccion = direccion; }
}