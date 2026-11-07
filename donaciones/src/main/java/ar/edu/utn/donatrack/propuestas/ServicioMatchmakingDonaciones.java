package ar.edu.utn.donatrack.propuestas;

import ar.edu.utn.donatrack.beneficiarios.BeneficiariosRepository;
import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.DonacionesRepository;
import ar.edu.utn.donatrack.donaciones.EstadoDonacion;
import ar.edu.utn.donatrack.notificaciones.Notificacion;
import ar.edu.utn.donatrack.propuestas.matchmaking.AlgoritmoMatchmaking;
import ar.edu.utn.donatrack.propuestas.matchmaking.MatchCandidato;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
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
    // Las lecturas quedan en el borde del caso de uso: el cálculo recibe los datos ya leídos.
    Collection<EntidadBeneficiaria> beneficiarios = beneficiariosRepository.getBeneficiarios();
    List<Donacion> donacionesEntregadas = donacionesRepository.getDonacionesEntregadas();
    for (Donacion donacion : donacionesRepository.getDonacionesEnDeposito()) {
      PropuestaDeAsignacion propuesta =
          generarPropuesta(donacion, beneficiarios, donacionesEntregadas, fechaReferencia);
      propuestaRepository.save(propuesta);
    }
  }

  public PropuestaDeAsignacion ejecutarMatchmakingParaDonacion(
      Donacion donacion, LocalDate fechaReferencia) {
    validarEnDeposito(donacion);
    PropuestaDeAsignacion propuesta =
        generarPropuesta(
            donacion,
            beneficiariosRepository.getBeneficiarios(),
            donacionesRepository.getDonacionesEntregadas(),
            fechaReferencia);
    propuestaRepository.save(propuesta);
    return propuesta;
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

  public List<Notificacion> confirmarAsignacionAdministrativa(
      Donacion donacion, EntidadBeneficiaria entidadBeneficiaria, LocalDate fechaConfirmacion) {
    PropuestaDeAsignacion propuesta =
        propuestaRepository
            .findPendingByDonacion(donacion)
            .orElseThrow(
                () ->
                    new IllegalStateException("No hay una propuesta pendiente para la donación."));
    propuesta.confirmar(entidadBeneficiaria);
    // La propuesta vive en memoria y puede guardar una copia de la donación separada del contexto
    // de persistencia, así que la transición se aplica acá sobre la donación administrada: de ahí
    // salen las notificaciones que el llamador envía después de guardar.
    List<Notificacion> notificaciones = donacion.confirmarEntidadBeneficiaria(entidadBeneficiaria);
    propuestaRepository.remove(donacion);
    // La confirmación cambia el estado y la entidad de la donación: hay que guardarla para que
    // esos cambios lleguen a la base. Este camino no pasa por DonacionController.
    donacionesRepository.save(donacion);
    return notificaciones;
  }

  /**
   * Calcula la propuesta de la donación sin tocar ningún repositorio.
   *
   * @param donacion donación para la que se calcula la propuesta
   * @param beneficiarios entidades candidatas
   * @param donacionesEntregadas entregas previas que alimentan el cálculo
   * @param fechaReferencia fecha desde la que se calcula el ranking
   * @return la propuesta calculada, todavía sin persistir
   */
  private PropuestaDeAsignacion generarPropuesta(
      Donacion donacion,
      Collection<EntidadBeneficiaria> beneficiarios,
      Collection<Donacion> donacionesEntregadas,
      LocalDate fechaReferencia) {
    List<List<MatchCandidato>> rankings =
        generarRankings(donacion, beneficiarios, donacionesEntregadas, fechaReferencia);
    return new PropuestaDeAsignacion(donacion, rankings);
  }

  /**
   * Ejecuta cada algoritmo con los datos recibidos y devuelve un ranking por algoritmo.
   *
   * <p>La lectura de las entregas queda en el borde del caso de uso: cada algoritmo la recibe como
   * parámetro y sigue sin conocer el repositorio de donaciones.
   *
   * @param donacion donación para la que se calculan los rankings
   * @param beneficiarios entidades candidatas
   * @param donacionesEntregadas entregas previas que alimentan el cálculo
   * @param fechaReferencia fecha desde la que se calcula el ranking
   * @return las listas de candidatos, una por algoritmo
   */
  private List<List<MatchCandidato>> generarRankings(
      Donacion donacion,
      Collection<EntidadBeneficiaria> beneficiarios,
      Collection<Donacion> donacionesEntregadas,
      LocalDate fechaReferencia) {
    return algoritmos.stream()
        .map(
            algoritmo ->
                algoritmo.rankear(donacion, beneficiarios, fechaReferencia, donacionesEntregadas))
        .toList();
  }

  /**
   * Verifica que la donación esté en depósito, el único estado admitido para el matchmaking.
   *
   * @param donacion donación que se quiere rankear
   * @throws IllegalStateException si la donación no está en depósito
   */
  private void validarEnDeposito(Donacion donacion) {
    if (donacion.getEstado() != EstadoDonacion.EN_DEPOSITO) {
      throw new IllegalStateException(
          "El matchmaking sólo puede ejecutarse para donaciones en depósito.");
    }
  }
}
