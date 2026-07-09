package ar.edu.utn.donatrack.donantes;

public class PersonaHumana extends Donante {
  private final String nombre;
  private final String apellido;
  private Integer edad;
  private final Genero genero;
  private String direccion;

  public PersonaHumana(
      CorreoElectronico medioDeContacto,
      String nombre,
      String apellido,
      Integer edad,
      String numDocumento,
      TipoDocumento documento,
      Genero genero,
      String direccion) {
    super(medioDeContacto, numDocumento, documento);
    this.nombre = nombre;
    this.apellido = apellido;
    this.edad = edad;
    this.genero = genero;
    this.direccion = direccion;
  }

  public String getNombre() {
    return nombre;
  }

  public String getApellido() {
    return apellido;
  }

  public int getEdad() {
    return edad;
  }

  public Genero getGenero() {
    return genero;
  }

  public void setEdad(int edad) {
    this.edad = edad;
  }

  public void setDireccion(String direccion) {
    this.direccion = direccion;
  }
}
