package ar.edu.utn.donatrack.donaciones;

import ar.edu.utn.donatrack.beneficiarios.BeneficiariosRepository;
import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ServicioMatchmakingDonaciones {
  private final DonacionesRepository donacionesRepository;
  private final BeneficiariosRepository beneficiariosRepository;
  private final List<AlgoritmoMatchmaking> algoritmos;

  public ServicioMatchmakingDonaciones(
      DonacionesRepository donacionesRepository,
      BeneficiariosRepository beneficiariosRepository,
      List<AlgoritmoMatchmaking> algoritmos) {
    this.donacionesRepository = donacionesRepository;
    this.beneficiariosRepository = beneficiariosRepository;
    this.algoritmos = new ArrayList<>(algoritmos);
  }

  public void ejecutarMatchmakingDiario(LocalDate fechaReferencia) {
    donacionesRepository.getDonacionesEnDeposito().forEach(donacion -> recalcular(donacion, fechaReferencia));
  }

  public void confirmarAsignacionAdministrativa(
      Donacion donacion, EntidadBeneficiaria entidadBeneficiaria, LocalDate fechaConfirmacion) {
    donacion.confirmarEntidadBeneficiaria(entidadBeneficiaria);
    entidadBeneficiaria.registrarRecepcion(donacion, fechaConfirmacion);
  }

  private void recalcular(Donacion donacion, LocalDate fechaReferencia) {
    List<RankingMatchmaking> rankings =
        algoritmos.stream()
            .map(
                algoritmo ->
                    algoritmo.rankear(
                        donacion, beneficiariosRepository.getBeneficiarios(), fechaReferencia))
            .toList();
    donacion.actualizarResultadoMatchmaking(new ResultadoMatchmaking(coincidencias(rankings), rankings));
  }

  private List<MatchCandidato> coincidencias(List<RankingMatchmaking> rankings) {
    if (rankings.size() < 2) {
      return List.of();
    }

    Set<EntidadBeneficiaria> comunes = new HashSet<>(entidades(rankings.get(0)));
    rankings.stream().skip(1).forEach(ranking -> comunes.retainAll(entidades(ranking)));

    if (comunes.isEmpty()) {
      return List.of();
    }

    return rankings.get(0).getCandidatos().stream()
        .filter(candidato -> comunes.contains(candidato.getEntidadBeneficiaria()))
        .limit(10)
        .map(
            candidato ->
                new MatchCandidato(
                    candidato.getEntidadBeneficiaria(), puntajeTotal(candidato.getEntidadBeneficiaria(), rankings)))
        .toList();
  }

  private Set<EntidadBeneficiaria> entidades(RankingMatchmaking ranking) {
    return ranking.getCandidatos().stream()
        .map(MatchCandidato::getEntidadBeneficiaria)
        .collect(Collectors.toSet());
  }

  private int puntajeTotal(EntidadBeneficiaria entidadBeneficiaria, List<RankingMatchmaking> rankings) {
    return rankings.stream()
        .flatMap(ranking -> ranking.getCandidatos().stream())
        .filter(candidato -> candidato.getEntidadBeneficiaria().equals(entidadBeneficiaria))
        .mapToInt(MatchCandidato::getPuntaje)
        .sum();
  }
}
