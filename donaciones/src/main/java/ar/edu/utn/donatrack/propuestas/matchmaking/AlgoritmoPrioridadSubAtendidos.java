package ar.edu.utn.donatrack.propuestas.matchmaking;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.DonacionesRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class AlgoritmoPrioridadSubAtendidos extends AlgoritmoMatchmaking {
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
  protected List<MatchCandidato> ordenarCandidatos(
      Donacion donacion,
      Collection<EntidadBeneficiaria> entidadesBeneficiarias,
      LocalDate fechaReferencia) {
    LocalDate fechaDesde = fechaReferencia.minusMonths(3);
    List<MatchCandidato> candidatos = new ArrayList<>();

    for (EntidadBeneficiaria entidad : entidadesBeneficiarias) {
      if (!entidad.tieneNecesidadCompatible(donacion)) {
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
            .thenComparing(candidato -> candidato.getEntidadBeneficiaria().getId()));
    return candidatos;
  }
}
