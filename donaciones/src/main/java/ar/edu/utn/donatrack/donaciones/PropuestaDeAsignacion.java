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
    confirmar(elegida, donacion);
  }

  /**
   * Confirma la asignación sobre la donación indicada.
   *
   * <p>La propuesta vive en memoria y puede sobrevivir a la solicitud que la creó: la donación que
   * referencia queda separada del contexto de persistencia. El llamador que tenga la instancia
   * administrada la pasa acá para que la transición se aplique —y se guarde— sobre ella.
   *
   * @param elegida entidad beneficiaria elegida, que debe pertenecer al ranking
   * @param donacionParaConfirmar donación sobre la que se aplica la transición
   */
  public void confirmar(EntidadBeneficiaria elegida, Donacion donacionParaConfirmar) {
    if (!pendiente) {
      return; // no-op on double-confirm
    }
    if (!incluye(elegida)) {
      throw new EntidadFueraDeRankingException();
    }
    donacionParaConfirmar.confirmarEntidadBeneficiaria(elegida);
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
