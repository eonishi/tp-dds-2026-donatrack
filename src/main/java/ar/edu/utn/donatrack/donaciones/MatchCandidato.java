package ar.edu.utn.donatrack.donaciones;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;

public class MatchCandidato {
  private final EntidadBeneficiaria entidadBeneficiaria;
  private final int puntaje;

  public MatchCandidato(EntidadBeneficiaria entidadBeneficiaria, int puntaje) {
    this.entidadBeneficiaria = entidadBeneficiaria;
    this.puntaje = puntaje;
  }

  public EntidadBeneficiaria getEntidadBeneficiaria() {
    return entidadBeneficiaria;
  }

  public int getPuntaje() {
    return puntaje;
  }
}
