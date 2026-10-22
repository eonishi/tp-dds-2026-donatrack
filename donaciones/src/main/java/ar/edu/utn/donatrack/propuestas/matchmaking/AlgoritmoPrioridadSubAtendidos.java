package ar.edu.utn.donatrack.propuestas.matchmaking;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donaciones.Donacion;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class AlgoritmoPrioridadSubAtendidos extends AlgoritmoMatchmaking {
  /**
   * Donaciones ya entregadas que alimentan el puntaje.
   *
   * <p>Es un atributo del algoritmo, no una dependencia del repositorio de donaciones: el caso de
   * uso lo reemplaza con {@link #actualizarDonacionesEntregadas(Collection)} antes de cada ranking.
   * La copia guardada es inmutable y la lectura es volátil para que un ranking nunca vea una lista
   * a medio publicar cuando el servicio la refresca mientras se calcula.
   */
  private volatile Collection<Donacion> donacionesEntregadas = List.of();

  public AlgoritmoPrioridadSubAtendidos(Collection<Donacion> donacionesEntregadasEnUnLapso) {
    this.donacionesEntregadas = donacionesEntregadasEnUnLapso;
  }

  @Override
  public String getNombre() {
    return "prioridad-sub-atendidos";
  }

  @Override
  protected List<MatchCandidato> ordenarCandidatos(
      Donacion donacion,
      Collection<EntidadBeneficiaria> entidadesBeneficiarias,
      LocalDate fechaReferencia) {
    // Una sola lectura del atributo: el ranking trabaja con un historial consistente.
    Collection<Donacion> entregas = donacionesEntregadas;
    LocalDate fechaDesde = fechaReferencia.minusMonths(3);
    List<MatchCandidato> candidatos = new ArrayList<>();

    for (EntidadBeneficiaria entidad : entidadesBeneficiarias) {
      if (!entidad.tieneNecesidadCompatible(donacion)) {
        continue;
      }

      long recibidas = cantidadEntregadasA(entregas, entidad, fechaDesde);
      int puntaje = (int) Math.max(0, 100 - recibidas);
      candidatos.add(new MatchCandidato(entidad, puntaje));
    }

    candidatos.sort(
        Comparator.comparingInt(MatchCandidato::getPuntaje)
            .reversed()
            .thenComparing(candidato -> candidato.getEntidadBeneficiaria().getRazonSocial())
            .thenComparing(candidato -> candidato.getEntidadBeneficiaria().getId()));
    return candidatos;
  }

  /**
   * Cuenta las entregas de la ventana que recibió la entidad indicada.
   *
   * @param entregas donaciones ya entregadas que alimentan el cálculo
   * @param entidad entidad beneficiaria que se evalúa
   * @param fechaDesde primer día de la ventana
   * @return la cantidad de donaciones entregadas a la entidad en o después de {@code fechaDesde}
   */
  private long cantidadEntregadasA(
      Collection<Donacion> entregas, EntidadBeneficiaria entidad, LocalDate fechaDesde) {
    return entregas.stream()
        .filter(donacion -> donacion.fueEntregadaA(entidad, fechaDesde))
        .count();
  }
}
