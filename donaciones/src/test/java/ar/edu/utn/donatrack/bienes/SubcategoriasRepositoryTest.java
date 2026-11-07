package ar.edu.utn.donatrack.bienes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;

/**
 * Prueba el repositorio del catálogo de subcategorías: el alta de una entrada nueva, el listado, la
 * búsqueda por la clave natural y la resolución con caché de la solicitud.
 *
 * <p>La base en memoria es única por JVM y otras clases confirman filas de catálogo, así que cada
 * prueba usa un nombre único derivado de un UUID y no depende del contenido total del catálogo. La
 * tabla declara el nombre único: insertar una segunda fila con un nombre ya almacenado falla en la
 * base, y esa garantía es la que fija la prueba del duplicado.
 */
class SubcategoriasRepositoryTest implements SimplePersistenceTest {

  @Test
  void obtenerOrCrearPersisteUnaEntradaNuevaDisponibleEnElListadoYPorNombre() {
    String nombre = nombreUnico();

    new SubcategoriasRepository().obtenerOrCrear(nombre, Categoria.ALIMENTO, new HashMap<>());
    entityManager().flush();
    entityManager().clear();

    SubcategoriasRepository repositorio = new SubcategoriasRepository();
    assertTrue(
        repositorio.getSubcategorias().stream().anyMatch(s -> s.getNombre().equals(nombre)),
        "La entrada creada debe aparecer en el listado");
    SubCategoria encontrada = repositorio.buscarPorNombre(nombre).orElseThrow();
    assertEquals(Categoria.ALIMENTO, encontrada.getCategoria(), "La categoría debe persistirse");
  }

  @Test
  void unaSegundaSubcategoriaConElMismoNombreSeRechazaEnLaBase() {
    String nombre = nombreUnico();
    persist(new SubCategoria(nombre, Categoria.ALIMENTO));
    entityManager().flush();

    PersistenceException error =
        assertThrows(
            PersistenceException.class,
            () -> {
              persist(new SubCategoria(nombre, Categoria.MOBILIARIO));
              entityManager().flush();
            },
            "La restricción única sobre el nombre debe rechazar la fila duplicada");

    ConstraintViolationException violacion = violacionDeRestriccion(error);
    assertEquals(
        "23505",
        violacion.getSQLState(),
        "HSQLDB reporta el duplicado como violación de restricción única");
  }

  @Test
  void obtenerOrCrearConLaMismaCacheDevuelveLaMismaInstanciaEInsertaUnaSolaFila() {
    String nombre = nombreUnico();
    SubcategoriasRepository repositorio = new SubcategoriasRepository();
    Map<String, SubCategoria> cache = new HashMap<>();

    SubCategoria primera = repositorio.obtenerOrCrear(nombre, Categoria.ALIMENTO, cache);
    SubCategoria segunda = repositorio.obtenerOrCrear(nombre, Categoria.MOBILIARIO, cache);
    entityManager().flush();

    assertSame(
        primera, segunda, "El mismo nombre en una solicitud debe resolver a la misma instancia");
    assertEquals(
        Categoria.ALIMENTO,
        segunda.getCategoria(),
        "La categoría de la instancia cacheada es la de la primera resolución");
    assertEquals(1, filasConNombre(nombre), "La solicitud debe insertar una sola fila");
  }

  @Test
  void laPrimeraCategoriaConLaQueSeCreaUnNombreEsLaQuePersiste() {
    String nombre = nombreUnico();
    new SubcategoriasRepository().obtenerOrCrear(nombre, Categoria.ALIMENTO, new HashMap<>());
    entityManager().flush();
    entityManager().clear();

    SubCategoria posterior =
        new SubcategoriasRepository().obtenerOrCrear(nombre, Categoria.MOBILIARIO, new HashMap<>());

    assertEquals(
        Categoria.ALIMENTO,
        posterior.getCategoria(),
        "Una solicitud posterior debe recibir la categoría del catálogo, no la pedida");
    assertEquals(1, filasConNombre(nombre), "La segunda resolución no debe insertar otra fila");
  }

  private long filasConNombre(String nombre) {
    return createQuery("select s from SubCategoria s where s.nombre = :nombre", SubCategoria.class)
        .setParameter("nombre", nombre)
        .getResultList()
        .size();
  }

  private static String nombreUnico() {
    return "Catalogo-" + UUID.randomUUID();
  }

  private static ConstraintViolationException violacionDeRestriccion(Throwable error) {
    for (Throwable causa = error; causa != null; causa = causa.getCause()) {
      if (causa instanceof ConstraintViolationException) {
        return (ConstraintViolationException) causa;
      }
    }
    throw new AssertionError(
        "La excepción no expone la violación de restricción de Hibernate", error);
  }
}
