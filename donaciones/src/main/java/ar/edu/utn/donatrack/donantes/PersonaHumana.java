package ar.edu.utn.donatrack.donantes;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Entity
public class PersonaHumana extends Donante {
  private String nombre;
  private String apellido;
  private Integer edad;

  @Enumerated(EnumType.STRING)
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

  public PersonaHumana(
      UUID id,
      CorreoElectronico medioDeContacto,
      String nombre,
      String apellido,
      Integer edad,
      String numDocumento,
      TipoDocumento documento,
      Genero genero,
      String direccion) {
    super(id, medioDeContacto, numDocumento, documento);
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

  public Integer getEdad() {
    return edad;
  }

  public Genero getGenero() {
    return genero;
  }

  public String getDireccion() {
    return direccion;
  }

  public void setEdad(int edad) {
    this.edad = edad;
  }

  public void setDireccion(String direccion) {
    this.direccion = direccion;
  }

  public void actualizarDatosPersonales(
      String nombre, String apellido, Integer edad, Genero genero, String direccion) {
    this.nombre = nombre;
    this.apellido = apellido;
    this.edad = edad;
    this.genero = genero;
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
