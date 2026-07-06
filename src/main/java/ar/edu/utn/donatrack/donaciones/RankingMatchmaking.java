package ar.edu.utn.donatrack.donaciones;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RankingMatchmaking {
  private final String algoritmo;
  private final List<MatchCandidato> candidatos = new ArrayList<>();

  public RankingMatchmaking(String algoritmo, List<MatchCandidato> candidatos) {
    this.algoritmo = algoritmo;
    this.candidatos.addAll(candidatos);
  }

  public String getAlgoritmo() {
    return algoritmo;
  }

  public List<MatchCandidato> getCandidatos() {
    return Collections.unmodifiableList(candidatos);
  }
}
