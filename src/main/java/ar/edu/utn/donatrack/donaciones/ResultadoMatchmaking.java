package ar.edu.utn.donatrack.donaciones;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResultadoMatchmaking {
  private final List<MatchCandidato> coincidencias = new ArrayList<>();
  private final List<RankingMatchmaking> rankings = new ArrayList<>();

  public ResultadoMatchmaking(List<MatchCandidato> coincidencias, List<RankingMatchmaking> rankings) {
    if (coincidencias != null) {
      this.coincidencias.addAll(coincidencias);
    }
    this.rankings.addAll(rankings);
  }

  public List<MatchCandidato> getCoincidencias() {
    return Collections.unmodifiableList(coincidencias);
  }

  public List<RankingMatchmaking> getRankings() {
    return Collections.unmodifiableList(rankings);
  }

  public boolean tieneCoincidencias() {
    return !coincidencias.isEmpty();
  }
}
