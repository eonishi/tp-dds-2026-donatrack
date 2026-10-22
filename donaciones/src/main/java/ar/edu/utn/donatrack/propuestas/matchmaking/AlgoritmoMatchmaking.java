package ar.edu.utn.donatrack.propuestas.matchmaking;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donaciones.Donacion;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public abstract class AlgoritmoMatchmaking {
  static final int MAXIMO_CANDIDATOS = 10;

  public abstract String getNombre();

  /**
   * Rankea las entidades candidatas con el historial de entregas que ya tiene el algoritmo.
   *
   * <p>Es un template method: el tope de candidatos lo aplica esta clase y cada algoritmo solo
   * ordena. El algoritmo no consulta repositorios: las entregas previas, si las necesita, se
   * inyectan con {@link #actualizarDonacionesEntregadas(Collection)} y quedan guardadas en un
   * atributo del algoritmo.
   *
   * @param donacion donación a asignar
   * @param entidadesBeneficiarias entidades que se evalúan como candidatas
   * @param fechaReferencia fecha desde la que se calcula el ranking
   * @return los candidatos ordenados por el algoritmo, hasta {@link #MAXIMO_CANDIDATOS}
   */
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
