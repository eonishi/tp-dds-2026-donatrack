package ar.edu.utn.donatrack.logistica;

import ar.edu.utn.donatrack.persistencia.JpaRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * JPA-backed repository for truck management.
 *
 * <p>Deduplication relies on the patente, which is the identity of {@link Camion#equals}: a truck
 * whose patente is already registered does not replace the stored one, it is discarded.
 */
public class CamionRepository implements JpaRepository {

  /**
   * Registers a truck.
   *
   * <p>When a truck with the same patente (according to {@link Camion#equals}) is already stored,
   * the stored truck is kept and the new instance is ignored — the semantics of {@link
   * HashSet#add}.
   *
   * @param camion the truck to register (non-null)
   */
  public void agregar(Camion camion) {
    inTransaction(
        () -> {
          if (buscarPorPatente(camion.getPatente()).isEmpty()) {
            persist(camion);
          }
        });
  }

  /**
   * @return an unmodifiable copy of every stored truck
   */
  public List<Camion> listarTodos() {
    return List.copyOf(createQuery("select c from Camion c", Camion.class).getResultList());
  }

  /**
   * Finds a truck by its patente.
   *
   * @param patente the patente to look up (non-null)
   * @return an {@link Optional} with the truck when it exists, or {@link Optional#empty()} when it
   *     does not
   */
  public Optional<Camion> buscarPorPatente(String patente) {
    return createQuery("select c from Camion c where c.patente = :patente", Camion.class)
        .setParameter("patente", patente)
        .getResultList()
        .stream()
        .findFirst();
  }
}
