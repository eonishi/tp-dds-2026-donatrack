package ar.edu.utn.donatrack.logistica;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RutaTest {

  // ── Helpers ────────────────────────────────────────────────────────

  private static Camion crearCamionDummy() {
    return new Camion("ABC123", 45.0, 2.5, 5000.0);
  }

  private static Destino crearDestino(String razonSocial) {
    return new Destino(new EntidadBeneficiaria(razonSocial, "Calle " + razonSocial, "555-0000"));
  }

  // ── TDD Cycle 1: Creation + Validation ──────────────────────────

  @Test
  void crearRuta_ConDatosValidos_DebeTenerAtributosCorrectos() {
    Camion camion = crearCamionDummy();
    Destino d1 = crearDestino("Fundación A");
    Destino d2 = crearDestino("Fundación B");
    Destino d3 = crearDestino("Fundación C");
    List<Destino> destinos = List.of(d1, d2, d3);

    Ruta ruta = new Ruta(camion, destinos);

    assertNotNull(ruta.getId());
    assertSame(camion, ruta.getCamion());
    assertFalse(ruta.isEnCurso());
    List<Destino> obtenidos = ruta.getDestinos();
    assertEquals(3, obtenidos.size());
    assertSame(d1, obtenidos.get(0));
    assertSame(d2, obtenidos.get(1));
    assertSame(d3, obtenidos.get(2));
  }

  @Test
  void crearRuta_ConCamionNull_DebeLanzarNullPointerException() {
    List<Destino> destinos = List.of(crearDestino("Fundación A"));
    assertThrows(NullPointerException.class, () -> new Ruta(null, destinos));
  }

  @Test
  void crearRuta_ConDestinosNull_DebeLanzarNullPointerException() {
    Camion camion = crearCamionDummy();
    assertThrows(NullPointerException.class, () -> new Ruta(camion, null));
  }

  @Test
  void crearRuta_ConDestinosDuplicados_DebeLanzarIllegalArgumentException() {
    Camion camion = crearCamionDummy();
    Destino repetido = crearDestino("Fundación A");
    List<Destino> destinos = List.of(repetido, repetido);

    assertThrows(IllegalArgumentException.class, () -> new Ruta(camion, destinos));
  }

  @Test
  void getDestinos_AlModificarLista_DebeLanzarUnsupportedOperationException() {
    Camion camion = crearCamionDummy();
    List<Destino> destinos = List.of(crearDestino("A"), crearDestino("B"), crearDestino("C"));
    Ruta ruta = new Ruta(camion, destinos);

    assertThrows(UnsupportedOperationException.class, () -> ruta.getDestinos().add(null));
  }

  // ── TDD Cycle 2: Lifecycle ────────────────────────────────────

  @Test
  void iniciar_CuandoNoEnCurso_DebeCambiarAtrue() {
    Ruta ruta = new Ruta(crearCamionDummy(), List.of(crearDestino("A")));

    ruta.iniciar();

    assertTrue(ruta.isEnCurso());
  }

  @Test
  void iniciar_CuandoYaEnCurso_DebeLanzarIllegalStateException() {
    Ruta ruta = new Ruta(crearCamionDummy(), List.of(crearDestino("A")));
    ruta.iniciar();

    assertThrows(IllegalStateException.class, ruta::iniciar);
    assertTrue(ruta.isEnCurso());
  }

  @Test
  void finalizar_CuandoEnCurso_DebeCambiarAfalse() {
    Ruta ruta = new Ruta(crearCamionDummy(), List.of(crearDestino("A")));
    ruta.iniciar();

    ruta.finalizar();

    assertFalse(ruta.isEnCurso());
  }

  @Test
  void finalizar_CuandoNoEnCurso_DebeLanzarIllegalStateException() {
    Ruta ruta = new Ruta(crearCamionDummy(), List.of(crearDestino("A")));

    assertThrows(IllegalStateException.class, ruta::finalizar);
    assertFalse(ruta.isEnCurso());
  }

  @Test
  void mismaUuid_EqualsDevuelveTrue() throws Exception {
    Ruta a = new Ruta(crearCamionDummy(), List.of(crearDestino("A")));
    Ruta b = new Ruta(crearCamionDummy(), List.of(crearDestino("B")));

    java.lang.reflect.Field field = Ruta.class.getDeclaredField("id");
    field.setAccessible(true);
    field.set(b, a.getId());

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void distintaUuid_EqualsDevuelveFalse() {
    Ruta a = new Ruta(crearCamionDummy(), List.of(crearDestino("A")));
    Ruta b = new Ruta(crearCamionDummy(), List.of(crearDestino("B")));

    assertNotEquals(a, b);
  }

  // ── TDD Cycle 3: Repository ────────────────────────────────────

  @Test
  void agregarRuta_LuegoBuscarPorId_DevuelveRuta() {
    RutaRepository repo = new RutaRepository();
    Ruta ruta = new Ruta(crearCamionDummy(), List.of(crearDestino("A")));

    repo.agregar(ruta);
    Optional<Ruta> resultado = repo.buscarPorId(ruta.getId());

    assertTrue(resultado.isPresent());
    assertSame(ruta, resultado.get());
  }

  @Test
  void buscarPorId_IdNoExistente_DevuelveOptionalEmpty() {
    RutaRepository repo = new RutaRepository();

    Optional<Ruta> resultado = repo.buscarPorId(UUID.randomUUID());

    assertTrue(resultado.isEmpty());
  }

  @Test
  void listarTodas_Con3Rutas_DevuelveLas3() {
    RutaRepository repo = new RutaRepository();
    Ruta r1 = new Ruta(crearCamionDummy(), List.of(crearDestino("A")));
    Ruta r2 = new Ruta(crearCamionDummy(), List.of(crearDestino("B")));
    Ruta r3 = new Ruta(crearCamionDummy(), List.of(crearDestino("C")));

    repo.agregar(r1);
    repo.agregar(r2);
    repo.agregar(r3);

    List<Ruta> todas = repo.listarTodas();
    assertEquals(3, todas.size());
    assertTrue(todas.contains(r1));
    assertTrue(todas.contains(r2));
    assertTrue(todas.contains(r3));
  }
}
