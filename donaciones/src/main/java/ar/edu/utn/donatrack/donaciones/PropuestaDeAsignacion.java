package ar.edu.utn.donatrack.donaciones;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import java.util.List;
import java.util.Objects;

public class PropuestaDeAsignacion {
  private final Donacion donacion;
  private final List<RankingMatchmaking> rankings;
  private boolean pendiente = true;

  public PropuestaDeAsignacion(Donacion donacion, List<RankingMatchmaking> rankings) {
    this.donacion = Objects.requireNonNull(donacion, "donacion no puede ser null");
    this.rankings = List.copyOf(rankings);
  }

  public void confirmar(EntidadBeneficiaria elegida) {
    if (!pendiente) {
      return; // no-op on double-confirm
    }
    if (!incluye(elegida)) {
      throw new EntidadFueraDeRankingException();
    }
    this.donacion.confirmarEntidadBeneficiaria(elegida);
    this.pendiente = false;
  }

  private boolean incluye(EntidadBeneficiaria entidad) {
    return rankings.stream()
        .flatMap(ranking -> ranking.getCandidatos().stream())
        .anyMatch(candidato -> candidato.getEntidadBeneficiaria().getId().equals(entidad.getId()));
  }

  public Donacion getDonacion() {
    return donacion;
  }

  public List<RankingMatchmaking> getRankings() {
    return rankings;
  }

  public boolean isPendiente() {
    return pendiente;
  }
}
