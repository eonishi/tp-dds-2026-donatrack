package ar.edu.utn.donatrack.logistica;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CamionTest {

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
  }

  @Test
  void buscarPorPatente_PatenteNoExistente_DevuelveOptionalEmpty() {
    CamionRepository repo = new CamionRepository();

    Optional<Camion> resultado = repo.buscarPorPatente("NOEXISTE");

    assertTrue(resultado.isEmpty());
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
  }
}
