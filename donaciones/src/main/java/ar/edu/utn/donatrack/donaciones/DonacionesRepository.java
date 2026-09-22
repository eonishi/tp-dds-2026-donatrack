package ar.edu.utn.donatrack.donaciones;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.persistencia.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de donaciones respaldado por JPA: las lecturas consultan la base y las escrituras
 * persisten en ella.
 *
 * <p>El estado de la donación se guarda en una columna: las consultas filtran por el estado actual
 * sin recorrer el historial.
 */
public class DonacionesRepository implements JpaRepository {

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
    return donacionesConEstado(EstadoDonacion.EN_DEPOSITO);
  }

  /**
   * @return una instantánea inmutable de las donaciones almacenadas cuyo estado actual es
   *     ASIGNACION_REALIZADA.
   */
  public List<Donacion> getDonacionesEnAsignacionRealizada() {
    return donacionesConEstado(EstadoDonacion.ASIGNACION_REALIZADA);
  }

  /**
   * Selecciona las donaciones cuyo estado actual es {@code estado}.
   *
   * <p>Consulta directa sobre la columna materializada: el historial queda como trazabilidad de
   * auditoría, no como fuente del estado.
   */
  private List<Donacion> donacionesConEstado(EstadoDonacion estado) {
    return List.copyOf(
        createQuery("SELECT d FROM Donacion d WHERE d.estado = :estado", Donacion.class)
            .setParameter("estado", estado)
            .getResultList());
  }

  /**
   * Registra una donación persistiéndola en la base.
   *
   * <p>La donación cascadea a sus bienes y a su historial, pero no a sus referencias externas: el
   * donante, la subcategoría de cada bien y la entidad beneficiaria deben existir antes de
   * persistirla. {@link #sincronizarReferencias(Donacion)} almacena el donante y las subcategorías
   * cuando todavía no tienen fila; la entidad beneficiaria debe llegar administrada por {@code
   * BeneficiariosRepository}, que ya es el repositorio dueño de ese agregado.
   *
   * @param donacion la donación a registrar
   */
  public void addDonacion(Donacion donacion) {
    inTransaction(
        () -> {
          sincronizarReferencias(donacion);
          persist(donacion);
        });
  }

  /**
   * Inserta o actualiza la donación. Si todavía no existe, la inserta; si ya está administrada por
   * el contexto de persistencia, la devuelve para que el vaciado de la transacción guarde sus
   * cambios; si está separada, fusiona su estado con la fila almacenada.
   *
   * @param donacion la donación a guardar
   * @return la instancia administrada resultante
   */
  public Donacion save(Donacion donacion) {
    return inTransaction(
        () -> {
          sincronizarReferencias(donacion);
          if (entityManager().contains(donacion)) {
            return donacion;
          }
          return merge(donacion);
        });
  }

  /**
   * Busca una donación por su identidad.
   *
   * @param id identidad de la donación
   * @return la donación almacenada, o {@link Optional#empty()} si no existe
   */
  public Optional<Donacion> findById(DonacionId id) {
    return Optional.ofNullable(find(Donacion.class, id.value()));
  }

  /**
   * Elimina la donación indicada junto con su historial, sus bienes y sus fotos. Si no existe, no
   * hace nada.
   *
   * @param id identidad de la donación
   */
  public void deleteById(DonacionId id) {
    inTransaction(
        () -> {
          Donacion donacion = find(Donacion.class, id.value());
          if (donacion != null) {
            remove(donacion);
          }
        });
  }

  /**
   * Cuenta las donaciones entregadas a la entidad desde la fecha indicada (inclusive).
   *
   * @param entidad entidad beneficiaria receptora
   * @param fechaDesde fecha mínima de entrega
   * @return cantidad de donaciones con un evento {@link EstadoDonacion#ENTREGADA} en el período
   */
  public long cantidadEntregadasDesde(EntidadBeneficiaria entidad, LocalDate fechaDesde) {
    // DISTINCT: la donación se cuenta una vez aunque la ventana contenga varias transiciones.
    String jpql =
        "SELECT COUNT(DISTINCT d) FROM Donacion d "
            + "JOIN d.historialEstado h "
            + "WHERE d.entidadBeneficiaria = :entidad "
            + "AND h.estadoAlcanzado = :estado "
            + "AND h.fecha >= :fechaDesde";

    return entityManager()
        .createQuery(jpql, Long.class)
        .setParameter("entidad", entidad)
        .setParameter("estado", EstadoDonacion.ENTREGADA)
        .setParameter("fechaDesde", fechaDesde.atStartOfDay())
        .getSingleResult();
  }

  /**
   * Asegura que las referencias externas de la donación tengan fila en la base antes de escribirla.
   *
   * <p>El donante pertenece a un agregado cuyo repositorio todavía no es JPA y las subcategorías de
   * los bienes las compone la API por solicitud: la donación solo cascadea a sus bienes y a su
   * historial, así que la clave foránea no puede escribirse sin esas filas. Este paso las inserta
   * cuando faltan y no hace nada cuando ya existen o están administradas. La entidad beneficiaria
   * no se escribe acá: su fila la garantiza {@code BeneficiariosRepository} y la donación
   * referencia la instancia administrada que ese repositorio devuelve.
   */
  private void sincronizarReferencias(Donacion donacion) {
    asegurarFila(donacion.getDonante());
    donacion.getBienes().forEach(bien -> asegurarFila(bien.getSubcategoria()));
  }

  private void asegurarFila(Object entidad) {
    if (entidad == null || entityManager().contains(entidad)) {
      return;
    }
    Object id =
        entityManager().getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entidad);
    if (id == null) {
      // Entidad nueva con identidad generada: persist la deja administrada y le asigna el id.
      persist(entidad);
    } else if (find(entidad.getClass(), id) == null) {
      // Entidad separada con identidad asignada: merge inserta la fila que falta.
      merge(entidad);
    }
  }
}
