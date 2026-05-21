package ar.edu.utn.donatrack.DonantesYContacto;

import java.util.List;
import java.util.ArrayList;

public class PersonaHumana extends Donante {
  private String nombre;
  private String apellido;
  private int edad;
  private String genero;
  private String direccion;

  // Constructor clásico (retrocompatible)
  public PersonaHumana(MedioDeContacto medioDeContacto, String nombre, String apellido, int edad, long numDocumento, TipoDocumento documento, String genero, String direccion) {
    super(medioDeContacto, numDocumento, documento);
    this.nombre = nombre;
    this.apellido = apellido;
    this.edad = edad;
    this.genero = genero;
    this.direccion = direccion;
  }

  // Constructor completo alineado a los requisitos (Email obligatorio, medios opcionales y medio predeterminado)
  public PersonaHumana(CorreoElectronico emailObligatorio, List<MedioDeContacto> mediosOpcionales, MedioDeContacto medioPredeterminado,
                       String nombre, String apellido, int edad, long numDocumento, TipoDocumento documento, String genero, String direccion) {
    super(emailObligatorio, numDocumento, documento);
    this.nombre = nombre;
    this.apellido = apellido;
    this.edad = edad;
    this.genero = genero;
    this.direccion = direccion;

    if (mediosOpcionales != null) {
      for (MedioDeContacto medio : mediosOpcionales) {
        this.agregarMedioDeContacto(medio);
      }
    }

    if (medioPredeterminado != null) {
      this.setMedioContactoPredeterminado(medioPredeterminado);
    } else {
      this.setMedioContactoPredeterminado(emailObligatorio);
    }
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

  public String getGenero() {
    return genero;
  }

  public String getDireccion() {
    return direccion;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public void setApellido(String apellido) {
    this.apellido = apellido;
  }

  public void setEdad(int edad) {
    this.edad = edad;
  }

  public void setGenero(String genero) {
    this.genero = genero;
  }

  public void setDireccion(String direccion) {
    this.direccion = direccion;
  }
}
