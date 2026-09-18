package ar.edu.utn.donatrack.donaciones;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.persistencia.JpaRepository;
import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio de donaciones respaldado por JPA: las lecturas consultan la base y las escrituras
 * persisten en ella.
 *
 * <p>El estado de la donación se guarda en una columna: las consultas de abajo filtran por el
 * estado actual sin recorrer el historial.
 */
public class DonacionesRepository implements JpaRepository {

  public long contarDonacionesAsignadas(EntidadBeneficiaria entidad, LocalDate fechaDesde) {
    // DISTINCT: una donación puede calificar dos veces en la ventana (asignada y luego entregada).
    String jpql =
        "SELECT COUNT(DISTINCT d) FROM Donacion d "
            + "JOIN d.historialEstado h "
            + "WHERE d.entidadBeneficiaria = :entidad "
            + "AND h.estadoAlcanzado IN (:estados) "
            + "AND h.fecha >= :fechaDesde";

    return entityManager()
        .createQuery(jpql, Long.class)
        .setParameter("entidad", entidad)
        .setParameter(
            "estados", List.of(EstadoDonacion.ASIGNACION_REALIZADA, EstadoDonacion.ENTREGADA))
        .setParameter("fechaDesde", fechaDesde)
        .getSingleResult();
  }

  /**
   * @return una instantánea inmutable de todas las donaciones almacenadas.
   */
  public List<Donacion> getDonaciones() {
    return List.copyOf(createQuery("select d from Donacion d", Donacion.class).getResultList());
  }

  /**
   * @return una instantánea inmutable de las donaciones almacenadas cuyo estado actual es
   *     EN_DEPOSITO.
   */
  public List<Donacion> getDonacionesEnDeposito() {
    return donationsWithLatestState(EstadoDonacion.EN_DEPOSITO);
  }

  /**
   * @return una instantánea inmutable de las donaciones almacenadas cuyo estado actual es
   *     ASIGNACION_REALIZADA.
   */
  public List<Donacion> getDonacionesEnAsignacionRealizada() {
    return donationsWithLatestState(EstadoDonacion.ASIGNACION_REALIZADA);
  }

  /**
   * Selecciona las donaciones cuyo estado actual es {@code estado}.
   *
   * <p>Consulta directa sobre la columna materializada: el historial queda como trazabilidad de
   * auditoría, no como fuente del estado.
   */
  private List<Donacion> donationsWithLatestState(EstadoDonacion estado) {
    return List.copyOf(
        createQuery("SELECT d FROM Donacion d WHERE d.estado = :estado", Donacion.class)
            .setParameter("estado", estado)
            .getResultList());
  }

  /**
   * Registra una donación y asienta su transición inicial EN_DEPOSITO.
   *
   * <p>Invariante: toda donación registrada a través de este repositorio tiene al menos la
   * transición inicial EN_DEPOSITO en la base, que documenta cómo empezó su historial. La llamada
   * es idempotente, así que una donación que ya transitó antes de ser registrada conserva su propio
   * historial.
   *
   * @param donacion la donación a registrar
   */
  public void addDonacion(Donacion donacion) {
    inTransaction(
        () -> {
          donacion.registrarEnDeposito(LocalDate.now());
          persist(donacion);
        });
  }
}
