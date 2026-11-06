package ar.edu.utn.donatrack.logistica;

import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de gestión de camiones respaldado por JPA.
 *
 * <p>La deduplicación se apoya en la patente, que es la identidad de {@link Camion#equals}: un
 * camión cuya patente ya está registrada no reemplaza al almacenado, sino que se descarta.
 */
public class CamionRepository implements WithSimplePersistenceUnit {

  /**
   * Registra un camión.
   *
   * <p>Cuando ya hay almacenado un camión con la misma patente (según {@link Camion#equals}), se
   * conserva el camión almacenado y se ignora la instancia nueva —la semántica de {@link
   * HashSet#add}.
   *
   * <p>Requiere una transacción activa del llamador (el caso de uso); nunca abre ni confirma una
   * transacción por su cuenta.
   *
   * @param camion el camión a registrar (no nulo)
   */
  public void agregar(Camion camion) {
    if (buscarPorPatente(camion.getPatente()).isEmpty()) {
      persist(camion);
    }
  }

  /**
   * @return copia inmodificable de todos los camiones almacenados
   */
  public List<Camion> listarTodos() {
    return List.copyOf(createQuery("select c from Camion c", Camion.class).getResultList());
  }

  /**
   * Busca un camión por su patente.
   *
   * @param patente la patente a buscar (no nula)
   * @return un {@link Optional} con el camión si existe, o {@link Optional#empty()} si no existe
   */
  public Optional<Camion> buscarPorPatente(String patente) {
    return createQuery("select c from Camion c where c.patente = :patente", Camion.class)
        .setParameter("patente", patente)
        .getResultList()
        .stream()
        .findFirst();
  }

  /**
   * Reemplaza los datos de un camión existente, identificado por su patente.
   *
   * <p>Requiere una transacción activa del llamador.
   *
   * @param camion el camión con los datos nuevos
   * @return el camión administrado con los datos actualizados
   */
  public Camion actualizar(Camion camion) {
    return merge(camion);
  }

  /**
   * Elimina un camión por su patente. Si no existe, no hace nada.
   *
   * <p>Requiere una transacción activa del llamador (el caso de uso); nunca abre ni confirma una
   * transacción por su cuenta.
   *
   * @param patente la patente del camión a eliminar
   */
  public void eliminar(String patente) {
    buscarPorPatente(patente).ifPresent(this::remove);
  }
}
