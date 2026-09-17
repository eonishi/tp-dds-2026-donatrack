package ar.edu.utn.donatrack.logistica;

import static org.junit.jupiter.api.Assertions.*;

import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CamionTest implements SimplePersistenceTest {

  // ── Entity Creation Tests ──────────────────────────────────────────

  @Test
  void crearCamion_ConDatosValidos_DebeTenerAtributosCorrectos() {
    Camion camion = new Camion("ABC123", 45.0, 2.5, 5000.0);

    assertEquals("ABC123", camion.getPatente());
    assertEquals(45.0, camion.getVolumenM3(), 0.001);
    assertEquals(2.5, camion.getAlturaM(), 0.001);
    assertEquals(5000.0, camion.getCapacidadCargaKg(), 0.001);
  }

  @Test
  void crearCamion_ConPatenteNull_DebeLanzarNullPointerException() {
    assertThrows(NullPointerException.class, () -> new Camion(null, 45.0, 2.5, 5000.0));
  }

  @Test
  void crearCamion_ConVolumenNegativo_DebeLanzarIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> new Camion("ABC123", -1.0, 2.5, 5000.0));
  }

  @Test
  void crearCamion_ConAlturaNaN_DebeLanzarIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class, () -> new Camion("ABC123", 45.0, Double.NaN, 5000.0));
  }

  @Test
  void crearCamion_ConCapacidadNegativa_DebeLanzarIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> new Camion("ABC123", 45.0, 2.5, -100.0));
  }

  // ── Equality Tests (Task 2.2) ──────────────────────────────────────

  @Test
  void mismaPatente_SeDeduplicaEnHashSet() {
    Camion a = new Camion("ABC123", 45.0, 2.5, 5000.0);
    Camion b = new Camion("ABC123", 50.0, 3.0, 6000.0);

    Set<Camion> set = new HashSet<>();
    set.add(a);
    set.add(b);

    assertEquals(1, set.size());
  }

  @Test
  void distintaPatente_NoSeDeduplicaEnHashSet() {
    Camion a = new Camion("ABC123", 45.0, 2.5, 5000.0);
    Camion b = new Camion("XYZ789", 45.0, 2.5, 5000.0);

    Set<Camion> set = new HashSet<>();
    set.add(a);
    set.add(b);

    assertEquals(2, set.size());
  }

  @Test
  void mismaPatente_EqualsDevuelveTrue() {
    Camion a = new Camion("ABC123", 45.0, 2.5, 5000.0);
    Camion b = new Camion("ABC123", 50.0, 3.0, 6000.0);

    assertEquals(a, b);
  }

  @Test
  void distintaPatente_EqualsDevuelveFalse() {
    Camion a = new Camion("ABC123", 45.0, 2.5, 5000.0);
    Camion b = new Camion("XYZ789", 45.0, 2.5, 5000.0);

    assertNotEquals(a, b);
  }

  @Test
  void mismaPatente_HashCodeEsIgual() {
    Camion a = new Camion("ABC123", 45.0, 2.5, 5000.0);
    Camion b = new Camion("ABC123", 50.0, 3.0, 6000.0);

    assertEquals(a.hashCode(), b.hashCode());
  }

  // ── Repository Tests (Tasks 2.3 & 2.4) ────────────────────────────

  @Test
  void agregarCamion_LuegoBuscarPorPatente_DevuelveCamion() {
    CamionRepository repo = new CamionRepository();
    Camion camion = new Camion("ABC123", 45.0, 2.5, 5000.0);

    repo.agregar(camion);
    Optional<Camion> resultado = repo.buscarPorPatente("ABC123");

    assertTrue(resultado.isPresent());
    assertEquals("ABC123", resultado.get().getPatente());

    // Round trip: a fresh repository must find the camion in the database.
    entityManager().clear();
    Optional<Camion> recargado = new CamionRepository().buscarPorPatente("ABC123");
    assertTrue(recargado.isPresent(), "El camión debe leerse de la base");
    assertEquals(45.0, recargado.get().getVolumenM3(), 0.001, "El camión debe conservar sus datos");
  }

  @Test
  void buscarPorPatente_PatenteNoExistente_DevuelveOptionalEmpty() {
    CamionRepository repo = new CamionRepository();

    Optional<Camion> resultado = repo.buscarPorPatente("NOEXISTE");

    assertTrue(resultado.isEmpty());
  }

  // ── listarTodos Tests ─────────────────────────────────────────────

  @Test
  void listarTodos_ConVariosCamiones_DevuelveTodos() {
    CamionRepository repo = new CamionRepository();
    Camion a = new Camion("ABC123", 45.0, 2.5, 5000.0);
    Camion b = new Camion("XYZ789", 50.0, 3.0, 6000.0);
    Camion c = new Camion("DEF456", 40.0, 2.0, 4000.0);
    repo.agregar(a);
    repo.agregar(b);
    repo.agregar(c);

    var todos = repo.listarTodos();

    assertEquals(3, todos.size());
    assertTrue(todos.contains(a));
    assertTrue(todos.contains(b));
    assertTrue(todos.contains(c));

    // Round trip: all three camiones must be listed from the database.
    entityManager().clear();
    List<Camion> recargados = new CamionRepository().listarTodos();
    assertEquals(3, recargados.size(), "Los tres camiones deben persistir");
    assertTrue(contienePatente(recargados, "ABC123"), "Falta ABC123 en la base");
    assertTrue(contienePatente(recargados, "XYZ789"), "Falta XYZ789 en la base");
    assertTrue(contienePatente(recargados, "DEF456"), "Falta DEF456 en la base");
  }

  @Test
  void listarTodos_SinCamiones_DevuelveListaVacia() {
    CamionRepository repo = new CamionRepository();

    var todos = repo.listarTodos();

    assertTrue(todos.isEmpty());
  }

  @Test
  void listarTodos_DevuelveListaInmodificable() {
    CamionRepository repo = new CamionRepository();
    repo.agregar(new Camion("ABC123", 45.0, 2.5, 5000.0));

    var todos = repo.listarTodos();

    assertThrows(UnsupportedOperationException.class, () -> todos.add(null));
  }

  @Test
  void agregarCamion_PatenteDuplicada_NoAumentaTamanio() {
    CamionRepository repo = new CamionRepository();
    Camion a = new Camion("ABC123", 45.0, 2.5, 5000.0);
    Camion b = new Camion("ABC123", 50.0, 3.0, 6000.0);

    repo.agregar(a);
    repo.agregar(b);

    Optional<Camion> resultado = repo.buscarPorPatente("ABC123");
    assertTrue(resultado.isPresent());
    assertEquals("ABC123", resultado.get().getPatente());
    assertEquals(1, repo.listarTodos().size(), "El camión duplicado no debe aumentar la colección");

    // Round trip: the database must keep one single row for the duplicated patente, holding the
    // first registered camion (HashSet#add does not replace an equal element).
    entityManager().clear();
    List<Camion> recargados = new CamionRepository().listarTodos();
    assertEquals(1, recargados.size(), "La patente duplicada no debe duplicarse en la base");
    assertEquals(
        45.0,
        recargados.get(0).getVolumenM3(),
        0.001,
        "El camión existente debe conservarse sin reemplazo");
  }

  private static boolean contienePatente(List<Camion> camiones, String patente) {
    return camiones.stream().anyMatch(c -> patente.equals(c.getPatente()));
  }
}
