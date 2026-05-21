package ar.edu.utn.donatrack.BeneficiariosYNecesidades;

import java.util.ArrayList;
import java.util.List;

public class EntidadBeneficiaria {
  private String razonSocial;
  private String direccion;
  private String telefono;

  List<NecesidadExtraordinaria> necesidadExtraordinaria = new ArrayList<>();
  List<NecesidadRecurrente> necesidadRecurrente = new ArrayList<>();

  List<String> correos = new ArrayList<>();

  public EntidadBeneficiaria(String razonSocial, String direccion, String telefono) {
    this.razonSocial = razonSocial;
    this.direccion = direccion;
    this.telefono = telefono;
  }

  public String getRazonSocial() {
    return razonSocial;
  }

  public void setRazonSocial(String razonSocial) {
    this.razonSocial = razonSocial;
  }

  public String getDireccion() {
    return direccion;
  }

  public void setDireccion(String direccion) {
    this.direccion = direccion;
  }

  public String getTelefono() {
    return telefono;
  }

  public void setTelefono(String telefono) {
    this.telefono = telefono;
  }

  public List<NecesidadExtraordinaria> getNecesidadExtraordinaria() {
    return necesidadExtraordinaria;
  }

  public List<NecesidadRecurrente> getNecesidadRecurrente() {
    return necesidadRecurrente;
  }

  public List<String> getCorreos() {
    return correos;
  }

  public void agregarNecesidadExtraordinaria(NecesidadExtraordinaria n) {
    this.necesidadExtraordinaria.add(n);
  }

  public void agregarNecesidadRecurrente(NecesidadRecurrente n) {
    this.necesidadRecurrente.add(n);
  }

  public void agregarCorreo(String correo) {
    this.correos.add(correo);
  }
}
