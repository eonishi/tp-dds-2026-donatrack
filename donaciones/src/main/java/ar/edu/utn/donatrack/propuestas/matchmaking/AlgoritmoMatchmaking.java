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
   * Rankea las entidades candidatas con las entregas previas que recibe como dato.
   *
   * <p>Es la única entrada del ranking: el tope de candidatos lo aplica esta clase y cada algoritmo
   * solo ordena. El algoritmo no consulta repositorios; la persistencia queda en el borde del caso
   * de uso y las entregas viajan como parámetro hasta el algoritmo que las necesita.
   *
   * @param donacion donación a asignar
   * @param entidadesBeneficiarias entidades que se evalúan como candidatas
   * @param fechaReferencia fecha desde la que se calcula el ranking
   * @param donacionesEntregadas donaciones ya entregadas que alimentan el cálculo
   * @return los candidatos ordenados por el algoritmo, hasta {@link #MAXIMO_CANDIDATOS}
   */
  public final List<MatchCandidato> rankear(
      Donacion donacion,
      Collection<EntidadBeneficiaria> entidadesBeneficiarias,
      LocalDate fechaReferencia,
      Collection<Donacion> donacionesEntregadas) {
    return ordenarCandidatos(
            donacion, entidadesBeneficiarias, fechaReferencia, donacionesEntregadas)
        .stream()
        .limit(MAXIMO_CANDIDATOS)
        .toList();
  }

  // Funcion qué sobreescribe cada algoritmo particular
  protected abstract List<MatchCandidato> ordenarCandidatos(
      Donacion donacion,
      Collection<EntidadBeneficiaria> entidadesBeneficiarias,
      LocalDate fechaReferencia,
      Collection<Donacion> donacionesEntregadas);
}
