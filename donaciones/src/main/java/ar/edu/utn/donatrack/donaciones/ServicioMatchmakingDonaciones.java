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

  public PropuestaDeAsignacion ejecutarMatchmakingParaDonacion(
      Donacion donacion, LocalDate fechaReferencia) {
    if (donacion.getNombreEstado() != NombreEstado.EN_DEPOSITO) {
      throw new IllegalStateException(
          "El matchmaking sólo puede ejecutarse para donaciones en depósito.");
    }
    propuestaRepository.remove(donacion);
    return calcularYGuardar(donacion, fechaReferencia);
  }

  public List<PropuestaDeAsignacion> ejecutarMatchmakingTotal(LocalDate fechaReferencia) {
    return donacionesRepository.getDonacionesEnDeposito().stream()
        .map(donacion -> ejecutarMatchmakingParaDonacion(donacion, fechaReferencia))
        .toList();
  }

  public PropuestaDeAsignacion obtenerRankingVigente(Donacion donacion) {
    return propuestaRepository
        .findPendingByDonacion(donacion)
        .orElseThrow(
            () -> new IllegalStateException("No hay un ranking vigente para la donación."));
  }

  public List<PropuestaDeAsignacion> obtenerPropuestasPendientes() {
    return propuestaRepository.findAllPending();
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

    calcularYGuardar(donacion, fechaReferencia);
  }

  private PropuestaDeAsignacion calcularYGuardar(Donacion donacion, LocalDate fechaReferencia) {
    List<RankingMatchmaking> rankings =
        algoritmos.stream()
            .map(
                algoritmo ->
                    algoritmo.rankear(
                        donacion, beneficiariosRepository.getBeneficiarios(), fechaReferencia))
            .toList();
    PropuestaDeAsignacion propuesta = new PropuestaDeAsignacion(donacion, rankings);
    propuestaRepository.save(propuesta);
    return propuesta;
  }
}
