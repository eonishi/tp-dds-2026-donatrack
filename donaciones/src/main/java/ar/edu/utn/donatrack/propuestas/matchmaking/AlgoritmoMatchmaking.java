package ar.edu.utn.donatrack.propuestas.matchmaking;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donaciones.Donacion;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public abstract class AlgoritmoMatchmaking {
  static final int MAXIMO_CANDIDATOS = 10;

  public abstract String getNombre();

  // template method
  public final List<MatchCandidato> rankear(
      Donacion donacion,
      Collection<EntidadBeneficiaria> entidadesBeneficiarias,
      LocalDate fechaReferencia) {
    return ordenarCandidatos(donacion, entidadesBeneficiarias, fechaReferencia).stream()
        .limit(MAXIMO_CANDIDATOS)
        .toList();
  }

  // Funcion qué sobreescribe cada algoritmo particular
  protected abstract List<MatchCandidato> ordenarCandidatos(
      Donacion donacion,
      Collection<EntidadBeneficiaria> entidadesBeneficiarias,
      LocalDate fechaReferencia);
}
