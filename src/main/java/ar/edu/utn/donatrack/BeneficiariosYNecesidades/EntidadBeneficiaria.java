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
}
