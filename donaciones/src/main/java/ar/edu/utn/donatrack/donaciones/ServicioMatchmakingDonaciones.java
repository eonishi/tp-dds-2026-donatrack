package ar.edu.utn.donatrack.donaciones;

import ar.edu.utn.donatrack.beneficiarios.BeneficiariosRepository;
import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ServicioMatchmakingDonaciones {
  private final DonacionesRepository donacionesRepository;
  private final BeneficiariosRepository beneficiariosRepository;
  private final PropuestaRepository propuestaRepository;
  private final List<AlgoritmoMatchmaking> algoritmos;

  public ServicioMatchmakingDonaciones(
      DonacionesRepository donacionesRepository,
      BeneficiariosRepository beneficiariosRepository,
      PropuestaRepository propuestaRepository,
      List<AlgoritmoMatchmaking> algoritmos) {
    this.donacionesRepository = donacionesRepository;
    this.beneficiariosRepository = beneficiariosRepository;
    this.propuestaRepository = propuestaRepository;
    this.algoritmos = new ArrayList<>(algoritmos);
  }

  public void ejecutarMatchmakingDiario(LocalDate fechaReferencia) {
    donacionesRepository
        .getDonacionesEnDeposito()
        .forEach(donacion -> recalcular(donacion, fechaReferencia));
  }

  public void confirmarAsignacionAdministrativa(
      Donacion donacion, EntidadBeneficiaria entidadBeneficiaria, LocalDate fechaConfirmacion) {
    PropuestaDeAsignacion propuesta =
        propuestaRepository
            .findPendingByDonacion(donacion)
            .orElseThrow(
                () ->
                    new IllegalStateException("No hay una propuesta pendiente para la donación."));
    propuesta.confirmar(entidadBeneficiaria);
    propuestaRepository.remove(donacion);
  }

  private void recalcular(Donacion donacion, LocalDate fechaReferencia) {
    if (propuestaRepository.findPendingByDonacion(donacion).isPresent()) {
      return; // skip Donaciones with existing pending Propuesta
    }

    List<RankingMatchmaking> rankings =
        algoritmos.stream()
            .map(
                algoritmo ->
                    algoritmo.rankear(
                        donacion, beneficiariosRepository.getBeneficiarios(), fechaReferencia))
            .toList();
    propuestaRepository.save(new PropuestaDeAsignacion(donacion, rankings));
  }
}
