package ar.edu.utn.donatrack.donaciones;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.persistencia.JpaRepository;
import java.time.LocalDate;
import java.util.List;

/**
 * JPA-backed repository of donations: reads query the database and writes persist there.
 *
 * <p>The donation state is not a column: it is derived from the historial, both when the entity is
 * loaded ({@link Donacion}'s {@code @PostLoad}) and in the queries below, which resolve the latest
 * historial entry.
 */
public class DonacionesRepository implements JpaRepository {

  public long contarDonacionesAsignadas(EntidadBeneficiaria entidad, LocalDate fechaDesde) {
    // DISTINCT: one donation can qualify twice in the window (assigned and later delivered).
    String jpql =
        "SELECT COUNT(DISTINCT d) FROM Donacion d "
            + "JOIN d.historialEstado h "
            + "WHERE d.entidadBeneficiaria = :entidad "
            + "AND h.estadoAlcanzado IN (:estados) "
            + "AND h.fecha >= :fechaDesde";

    return entityManager()
        .createQuery(jpql, Long.class)
        .setParameter("entidad", entidad)
        .setParameter("estados", List.of(NombreEstado.ASIGNACION_REALIZADA, NombreEstado.ENTREGADA))
        .setParameter("fechaDesde", fechaDesde)
        .getSingleResult();
  }

  /**
   * @return an immutable snapshot of every stored donation.
   */
  public List<Donacion> getDonaciones() {
    return List.copyOf(createQuery("select d from Donacion d", Donacion.class).getResultList());
  }

  /**
   * @return an immutable snapshot of the stored donations whose latest transition is EN_DEPOSITO.
   */
  public List<Donacion> getDonacionesEnDeposito() {
    return donationsWithLatestState(NombreEstado.EN_DEPOSITO);
  }

  /**
   * @return an immutable snapshot of the stored donations whose latest transition is
   *     ASIGNACION_REALIZADA.
   */
  public List<Donacion> getDonacionesEnAsignacionRealizada() {
    return donationsWithLatestState(NombreEstado.ASIGNACION_REALIZADA);
  }

  /**
   * Selects the donations whose latest historial entry is {@code estado}.
   *
   * <p>The latest entry is the one with the highest id: {@code fecha} is a date, so two transitions
   * of the same donation can share it and ordering by it would be wrong.
   */
  private List<Donacion> donationsWithLatestState(NombreEstado estado) {
    String jpql =
        "SELECT d FROM Donacion d "
            + "JOIN d.historialEstado h "
            + "WHERE h.estadoAlcanzado = :estado "
            + "AND h.id = (SELECT MAX(h2.id) FROM HistorialEstado h2 WHERE h2.donacion = d)";

    return List.copyOf(
        createQuery(jpql, Donacion.class).setParameter("estado", estado).getResultList());
  }

  /**
   * Registers a donation and records its initial EN_DEPOSITO transition.
   *
   * <p>Invariant: every donation registered through this repository has at least the initial
   * EN_DEPOSITO transition in the database. Without it the donation would have no historial to
   * derive its state from, and the state queries would never see it. The call is idempotent, so a
   * donation that already transitioned before being registered keeps its own history.
   *
   * @param donacion the donation to register
   */
  public void addDonacion(Donacion donacion) {
    inTransaction(
        () -> {
          donacion.registrarEnDeposito(LocalDate.now());
          persist(donacion);
        });
  }
}
