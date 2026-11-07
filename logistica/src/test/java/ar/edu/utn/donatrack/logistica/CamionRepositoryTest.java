package ar.edu.utn.donatrack.logistica;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import org.junit.jupiter.api.Test;

/**
 * Prueba el repositorio de camiones como repositorio JPA: guarda un camión, limpia el contexto de
 * persistencia, lo recarga por su identidad y por el listado, y lo elimina. Cada recarga usa un
 * repositorio nuevo para que la lectura tenga que salir de la base y no de una colección en
 * memoria.
 */
class CamionRepositoryTest implements SimplePersistenceTest {

  @Test
  void agregarRecargaYEliminaUnCamion() {
    Camion camion = new Camion("ABC123", 45.0, 2.5, 5000.0);

    new CamionRepository().agregar(camion);
    entityManager().flush();
    entityManager().clear();

    CamionRepository repositorio = new CamionRepository();
    Camion porIdentidad = repositorio.buscarPorPatente("ABC123").orElseThrow();

    assertEquals("ABC123", porIdentidad.getPatente());
    assertEquals(45.0, porIdentidad.getVolumenM3(), 0.001);
    assertEquals(2.5, porIdentidad.getAlturaM(), 0.001);
    assertEquals(5000.0, porIdentidad.getCapacidadCargaKg(), 0.001);

    var listado = repositorio.listarTodos();
    assertEquals(1, listado.size(), "El camión guardado debe aparecer en el listado");
    Camion porListado =
        listado.stream()
            .filter(c -> c.getPatente().equals("ABC123"))
            .findFirst()
            .orElseThrow(
                () -> new AssertionError("El camión debe aparecer en el listado del repositorio"));
    assertEquals(
        5000.0,
        porListado.getCapacidadCargaKg(),
        0.001,
        "La recarga desde el listado debe conservar los valores persistidos");

    repositorio.eliminar("ABC123");
    entityManager().flush();
    entityManager().clear();

    CamionRepository vacio = new CamionRepository();
    assertTrue(
        vacio.buscarPorPatente("ABC123").isEmpty(),
        "El camión eliminado no debe poder recargarse por su identidad");
    assertTrue(vacio.listarTodos().isEmpty(), "El camión eliminado no debe aparecer en el listado");
  }
}
