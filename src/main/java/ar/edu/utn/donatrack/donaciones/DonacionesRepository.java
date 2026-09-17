package ar.edu.utn.donatrack.donaciones;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.persistencia.JpaRepository;
import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio de donaciones respaldado por JPA: las lecturas consultan la base y las escrituras
 * persisten en ella.
 *
 * <p>El estado de la donación no es una columna: se deriva del historial, tanto al cargar la
 * entidad (el {@code @PostLoad} de {@link Donacion}) como en las consultas de abajo, que resuelven
 * la última entrada del historial.
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
        .setParameter("estados", List.of(NombreEstado.ASIGNACION_REALIZADA, NombreEstado.ENTREGADA))
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
   * @return una instantánea inmutable de las donaciones almacenadas cuya última transición es
   *     EN_DEPOSITO.
   */
  public List<Donacion> getDonacionesEnDeposito() {
    return donationsWithLatestState(NombreEstado.EN_DEPOSITO);
  }

  /**
   * @return una instantánea inmutable de las donaciones almacenadas cuya última transición es
   *     ASIGNACION_REALIZADA.
   */
  public List<Donacion> getDonacionesEnAsignacionRealizada() {
    return donationsWithLatestState(NombreEstado.ASIGNACION_REALIZADA);
  }

  /**
   * Selecciona las donaciones cuya última entrada del historial es {@code estado}.
   *
   * <p>La última entrada es la de mayor id: {@code fecha} es una fecha, así que dos transiciones de
   * la misma donación pueden compartirla y ordenar por ella sería incorrecto.
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
   * Registra una donación y asienta su transición inicial EN_DEPOSITO.
   *
   * <p>Invariante: toda donación registrada a través de este repositorio tiene al menos la
   * transición inicial EN_DEPOSITO en la base. Sin ella la donación no tendría historial del cual
   * derivar su estado, y las consultas por estado nunca la verían. La llamada es idempotente, así
   * que una donación que ya transitó antes de ser registrada conserva su propio historial.
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
