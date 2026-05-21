package ar.edu.utn.donatrack.BeneficiariosYNecesidades;

import java.util.ArrayList;
import java.util.List;

public class EntidadBeneficiaria {
  private final String razonSocial;
  private final String direccion;
  private final String telefono;
  List<NecesidadExtraordinaria> necesidadExtraordinaria = new ArrayList<>();
  List<NecesidadRecurrente> necesidadRecurrente = new ArrayList<>();
  List<String> correos = new ArrayList<>();

  public EntidadBeneficiaria(String razonSocial, String direccion, String telefono, List<String> correos) {
    this.razonSocial = razonSocial;
    this.direccion = direccion;
    this.telefono = telefono;
    this.correos.addAll(correos);
  }

  public void registrarNecesidadRecurrente(NecesidadRecurrente necesidad) {
    this.necesidadRecurrente.add(necesidad);
  }

  public void registrarNecesidadExtraordinario(NecesidadExtraordinaria necesidad) {
    this.necesidadExtraordinaria.add(necesidad);
  }
}
