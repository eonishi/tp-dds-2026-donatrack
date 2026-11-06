package ar.edu.utn.donatrack.donaciones;

import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de donaciones respaldado por JPA: las lecturas consultan la base y las escrituras
 * persisten en ella.
 *
 * <p>El estado de la donación se guarda en una columna: las consultas filtran por el estado actual
 * sin recorrer el historial.
 */
public class DonacionesRepository implements WithSimplePersistenceUnit {

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
   * @return una instantánea inmutable de las donaciones almacenadas cuyo estado actual es
   *     ENTREGADA.
   */
  public List<Donacion> getDonacionesEntregadas() {
    return donacionesConEstado(EstadoDonacion.ENTREGADA);
  }

  /**
   * Donaciones asignadas a una entidad beneficiaria, en cualquier estado. La vista decide cómo
   * agruparlas (por recibir, recibidas, con problemas).
   *
   * @param entidadId identidad de la entidad beneficiaria
   */
  public List<Donacion> getDonacionesDeEntidad(Long entidadId) {
    return List.copyOf(
        createQuery(
                "SELECT d FROM Donacion d WHERE d.entidadBeneficiaria.id = :entidad",
                Donacion.class)
            .setParameter("entidad", entidadId)
            .getResultList());
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
   * <p>Requiere una transacción activa del llamador (el caso de uso); nunca abre ni confirma una
   * transacción por su cuenta.
   *
   * <p>La donación cascadea a sus bienes y su historial se guarda con ella, pero no cascadea a sus
   * referencias externas: el donante, la subcategoría de cada bien y la entidad beneficiaria deben
   * existir antes de persistirla y llegar administrados por su repositorio dueño. Al donante lo
   * administra {@code DonantesRepository}, a la entidad beneficiaria {@code
   * BeneficiariosRepository} y a la subcategoría de cada bien {@code SubcategoriasRepository}, el
   * único repositorio que escribe el catálogo: este repositorio nunca crea entradas del catálogo
   * como efecto lateral de escribir la donación.
   *
   * @param donacion la donación a registrar
   */
  public void addDonacion(Donacion donacion) {
    persist(donacion);
  }

  /**
   * Inserta o actualiza la donación. Si todavía no existe, la inserta; si ya está administrada por
   * el contexto de persistencia, la devuelve para que el vaciado de la transacción guarde sus
   * cambios; si está separada, fusiona su estado con la fila almacenada.
   *
   * <p>Requiere una transacción activa del llamador (el caso de uso); nunca abre ni confirma una
   * transacción por su cuenta.
   *
   * <p>La subcategoría de cada bien debe existir y llegar administrada por {@code
   * SubcategoriasRepository}; este repositorio no escribe el catálogo.
   *
   * @param donacion la donación a guardar
   * @return la instancia administrada resultante
   */
  public Donacion save(Donacion donacion) {
    if (entityManager().contains(donacion)) {
      return donacion;
    }
    return merge(donacion);
  }

  /**
   * Busca una donación por su identidad.
   *
   * @param id identidad de la donación
   * @return la donación almacenada, o {@link Optional#empty()} si no existe
   */
  public Optional<Donacion> findById(UUID id) {
    return Optional.ofNullable(find(Donacion.class, id));
  }

  /**
   * Elimina la donación indicada junto con su historial, sus bienes y sus fotos. Si no existe, no
   * hace nada.
   *
   * <p>Requiere una transacción activa del llamador (el caso de uso); nunca abre ni confirma una
   * transacción por su cuenta.
   *
   * @param id identidad de la donación
   */
  public void deleteById(UUID id) {
    Donacion donacion = find(Donacion.class, id);
    if (donacion != null) {
      remove(donacion);
    }
  }
}
