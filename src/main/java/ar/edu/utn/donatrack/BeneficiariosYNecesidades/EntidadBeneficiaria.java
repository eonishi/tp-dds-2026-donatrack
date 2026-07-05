package ar.edu.utn.donatrack.BeneficiariosYNecesidades;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EntidadBeneficiaria {
  private String razonSocial;
  private String direccion;
  private String telefono;

  private final List<NecesidadExtraordinaria> necesidadesExtraordinarias = new ArrayList<>();
  private final List<NecesidadRecurrente> necesidadesRecurrentes = new ArrayList<>();
  private final List<String> correos = new ArrayList<>();

  public EntidadBeneficiaria(String razonSocial, String direccion, String telefono) {
    this.razonSocial = razonSocial;
    this.direccion = direccion;
    this.telefono = telefono;
  }

  public String getRazonSocial() { return razonSocial; }
  public String getDireccion() { return direccion; }
  public String getTelefono() { return telefono; }

  public List<NecesidadExtraordinaria> getNecesidadesExtraordinarias() {
      // innnecesario el unmodifiableList
    return Collections.unmodifiableList(necesidadesExtraordinarias);
  }

  public List<NecesidadRecurrente> getNecesidadesRecurrentes() {
    return Collections.unmodifiableList(necesidadesRecurrentes);
  }

  public List<String> getCorreos() {
    return Collections.unmodifiableList(correos);
  }

  public void agregarNecesidadExtraordinaria(NecesidadExtraordinaria n) {
    this.necesidadesExtraordinarias.add(n);
  }

  public void agregarNecesidadRecurrente(NecesidadRecurrente n) {
    this.necesidadesRecurrentes.add(n);
  }

  public void agregarCorreo(String correo) {
    this.correos.add(correo);
  }
}