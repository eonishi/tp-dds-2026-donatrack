package ar.edu.utn.donatrack.DonantesYContacto;

public class PersonaHumana extends Donante {
  private final String nombre;
  private final String apellido;
  private Integer edad;
  private String genero; // Quizas un enum (?)
  private String direccion;

  public PersonaHumana(
      MedioDeContacto medioDeContacto,
      String nombre,
      String apellido,
      Integer edad,
      String numDocumento,
      TipoDocumento documento,
      String genero,
      String direccion
  ) {
    super(validateMedioDeContacto(medioDeContacto), numDocumento, documento);
    this.nombre = nombre;
    this.apellido = apellido;
    this.edad = edad;
    this.genero = genero;
    this.direccion = direccion;
  }

  private static MedioDeContacto validateMedioDeContacto(MedioDeContacto medioDeContacto) { 
    if (!(medioDeContacto instanceof CorreoElectronico)) {
      throw new IllegalArgumentException("El medio de contacto obligatorio para una persona humana debe ser un Correo Electrónico.");
    }
    return medioDeContacto;
  }

  public String getNombre() { return nombre; }
  public String getApellido() { return apellido; }
  public int getEdad() { return edad; }

  public void setGenero(String genero) { this.genero = genero; }
  public void setEdad(int edad){ this.edad = edad; }
  public void setDireccion(String direccion){ this.direccion = direccion; }
}