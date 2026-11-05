package ar.edu.utn.donatrack.propuestas.matchmaking;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Embeddable
public class MatchCandidato {
  @ManyToOne
  @JoinColumn(name = "entidad_id", nullable = false)
  private EntidadBeneficiaria entidadBeneficiaria;

  @Column(nullable = false)
  private int puntaje;

  protected MatchCandidato() {}

  public MatchCandidato(EntidadBeneficiaria entidadBeneficiaria, int puntaje) {
    this.entidadBeneficiaria = entidadBeneficiaria;
    this.puntaje = puntaje;
  }

  public EntidadBeneficiaria getEntidadBeneficiaria() {
    return entidadBeneficiaria;
  }

  public boolean coincideBeneficiaria(EntidadBeneficiaria entidadElegida) {
    return entidadBeneficiaria.equals(entidadElegida);
  }

  public int getPuntaje() {
    return puntaje;
  }
}
