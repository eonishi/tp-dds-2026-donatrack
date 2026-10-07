package ar.edu.utn.donatrack.bienes;

import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repositorio del catálogo de subcategorías compartido entre la oferta ({@link Bien}) y la demanda
 * ({@code Necesidad}), respaldado por JPA: las lecturas consultan la base y las escrituras
 * persisten en ella.
 *
 * <p>La clave natural es {@code nombre}: {@link SubCategoria#equals(Object)} y {@link
 * SubCategoria#hashCode()} sólo miran el nombre, así que el nombre identifica una entrada del
 * catálogo. La base garantiza esa unicidad con la restricción {@code uk_subcategoria_nombre}; aun
 * así {@link #buscarPorNombre(String)} recorre la lista con {@code findFirst()} y nunca usa {@code
 * getSingleResult()}, que lanzaría {@code NonUniqueResultException} si una base preexistente
 * todavía tuviera filas duplicadas.
 *
 * <p>Toda escritura requiere una transacción activa del llamador (el caso de uso); el repositorio
 * nunca abre ni confirma una transacción por su cuenta.
 */
public class SubcategoriasRepository implements WithSimplePersistenceUnit {

  /**
   * @return una instantánea inmutable de todas las subcategorías almacenadas.
   */
  public List<SubCategoria> getSubcategorias() {
    return List.copyOf(
        createQuery("select s from SubCategoria s", SubCategoria.class).getResultList());
  }

  /**
   * Busca una subcategoría por su clave natural, el nombre.
   *
   * <p>Tolera filas duplicadas como defensa: la restricción {@code uk_subcategoria_nombre} impide
   * insertar dos filas con el mismo nombre, pero la consulta reduce la lista con {@code
   * findFirst()} en lugar de {@code getSingleResult()} para no fallar contra una base preexistente
   * que todavía tenga duplicados.
   *
   * @param nombre nombre de la subcategoría buscada
   * @return la primera subcategoría almacenada con ese nombre, o {@link Optional#empty()} si no
   *     existe ninguna
   */
  public Optional<SubCategoria> buscarPorNombre(String nombre) {
    return createQuery("select s from SubCategoria s where s.nombre = :nombre", SubCategoria.class)
        .setParameter("nombre", nombre)
        .getResultList()
        .stream()
        .findFirst();
  }

  /**
   * Devuelve la entrada del catálogo con el nombre indicado y la crea si falta, resolviendo el
   * mismo nombre siempre a la misma instancia dentro de la solicitud.
   *
   * <p>La primera categoría con la que se crea un nombre es la que persiste: si el catálogo ya
   * tiene la entrada, la categoría recibida se ignora y se devuelve la almacenada; si la entrada se
   * crea en esta llamada, queda con la categoría recibida y las llamadas posteriores de la misma
   * solicitud reciben la instancia cacheada aunque nombren otra categoría. Sin esta resolución, dos
   * instancias con el mismo nombre llegarían a la base y la segunda violaría la restricción única
   * del catálogo.
   *
   * <p>Requiere una transacción activa del llamador (el caso de uso): una entrada nueva se escribe
   * con {@code persist} y la fila debe unirse a esa transacción.
   *
   * @param nombre nombre de la subcategoría
   * @param categoria categoría con la que se crea la entrada si falta
   * @param cache caché local de la solicitud indexada por nombre; nunca se comparte entre
   *     solicitudes
   * @return la instancia administrada de la entrada del catálogo
   */
  public SubCategoria obtenerOrCrear(
      String nombre, Categoria categoria, Map<String, SubCategoria> cache) {
    return cache.computeIfAbsent(
        nombre,
        clave ->
            buscarPorNombre(clave)
                .orElseGet(
                    () -> {
                      SubCategoria nueva = new SubCategoria(clave, categoria);
                      persist(nueva);
                      return nueva;
                    }));
  }
}
