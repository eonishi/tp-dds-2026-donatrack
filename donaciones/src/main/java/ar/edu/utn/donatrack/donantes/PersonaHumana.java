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
      MedioDeContacto medioDeContacto,
      String nombre,
      String apellido,
      String numDocumento,
      TipoDocumento documento,
      Genero genero,
      String direccion) {
    super(MedioDeContacto.exigirCorreo(medioDeContacto), numDocumento, documento);
    this.nombre = nombre;
    this.apellido = apellido;
    this.genero = genero;
    this.direccion = direccion;
  }

  public PersonaHumana(
      UUID id,
      MedioDeContacto medioDeContacto,
      String nombre,
      String apellido,
      String numDocumento,
      TipoDocumento documento,
      Genero genero,
      String direccion) {
    super(id, MedioDeContacto.exigirCorreo(medioDeContacto), numDocumento, documento);
    this.nombre = nombre;
    this.apellido = apellido;
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

  public void setEdad(int edad) {
    if (edad < 0 || edad > 130) {
      throw new IllegalArgumentException("La edad debe estar entre 0 y 130.");
    }
    this.edad = edad;
  }

  public Genero getGenero() {
    return genero;
  }

  public String getDireccion() {
    return direccion;
  }

  public void setDireccion(String direccion) {
    this.direccion = direccion;
  }

  @Override
  protected void copiarDatosPropios(Donante datos) {
    PersonaHumana otra = (PersonaHumana) datos;
    this.nombre = otra.nombre;
    this.apellido = otra.apellido;
    this.genero = otra.genero;
    this.direccion = otra.direccion;
    if (otra.edad != null) {
      this.edad = otra.edad;
    }
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
