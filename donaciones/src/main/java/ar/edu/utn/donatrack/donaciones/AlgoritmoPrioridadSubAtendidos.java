package ar.edu.utn.donatrack.donaciones;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.beneficiarios.Necesidad;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class AlgoritmoPrioridadSubAtendidos implements AlgoritmoMatchmaking {
  private final DonacionesRepository donacionesRepository;

  public AlgoritmoPrioridadSubAtendidos(DonacionesRepository donacionesRepository) {
    this.donacionesRepository =
        Objects.requireNonNull(donacionesRepository, "donacionesRepository no puede ser null");
  }

  @Override
  public String getNombre() {
    return "prioridad-sub-atendidos";
  }

  @Override
  public RankingMatchmaking rankear(
      Donacion donacion,
      Collection<EntidadBeneficiaria> entidadesBeneficiarias,
      LocalDate fechaReferencia) {
    LocalDate fechaDesde = fechaReferencia.minusMonths(3);
    List<MatchCandidato> candidatos = new ArrayList<>();

    for (EntidadBeneficiaria entidad : entidadesBeneficiarias) {
      if (!tieneNecesidadCompatible(donacion, entidad)) {
        continue;
      }

      long recibidas = donacionesRepository.cantidadEntregadasDesde(entidad, fechaDesde);
      int puntaje = (int) Math.max(0, 100 - recibidas);
      candidatos.add(new MatchCandidato(entidad, puntaje));
    }

    candidatos.sort(
        Comparator.comparingInt(MatchCandidato::getPuntaje)
            .reversed()
            .thenComparing(candidato -> candidato.getEntidadBeneficiaria().getRazonSocial())
            .thenComparing(candidato -> candidato.getEntidadBeneficiaria().getId().value()));
    return new RankingMatchmaking(getNombre(), candidatos.stream().limit(10).toList());
  }

  private boolean tieneNecesidadCompatible(Donacion donacion, EntidadBeneficiaria entidad) {
    List<Necesidad> necesidades = new ArrayList<>(entidad.getNecesidadesExtraordinarias());
    necesidades.addAll(entidad.getNecesidadesRecurrentes());
    return necesidades.stream()
        .anyMatch(necesidad -> necesidad.getSubcategoria().equals(donacion.getSubcategoria()));
  }
}
