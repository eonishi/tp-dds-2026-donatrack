package ar.edu.utn.donatrack.donaciones;

import java.util.List;

/**
 * Empareja el nombre de un algoritmo con los candidatos que rankeó.
 *
 * <p>El emparejamiento se arma al construir la propuesta, no dentro del ranking que devuelve el
 * algoritmo.
 */
public record RankingAlgoritmo(String algoritmo, List<MatchCandidato> candidatos) {
  public RankingAlgoritmo {
    candidatos = List.copyOf(candidatos);
  }
}
