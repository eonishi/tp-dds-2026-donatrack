package ar.edu.utn.donatrack.beneficiarios;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BeneficiariosRepository {
  private final HashSet<EntidadBeneficiaria> beneficiarios = new HashSet<>();

  public Set<EntidadBeneficiaria> getBeneficiarios() {
    return Collections.unmodifiableSet(beneficiarios);
  }

  public void addBeneficiario(EntidadBeneficiaria nuevo) {
    this.beneficiarios.add(nuevo);
  }
}
