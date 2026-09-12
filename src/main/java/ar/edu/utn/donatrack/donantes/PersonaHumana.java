package ar.edu.utn.donatrack.donantes;
import javax.persistence.Entity;

@Entity 
public class PersonaHumana extends Donante {
  private String nombre;
  private String apellido;
  private Integer edad;
  private Genero genero;
  private String direccion;

  protected PersonaHumana() {}

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

  @Override
  void merge(Donante entrante) {
    super.merge(entrante);
    if (entrante instanceof PersonaHumana ph) {
      this.nombre = ph.nombre;
      this.apellido = ph.apellido;
    }
  }
}
