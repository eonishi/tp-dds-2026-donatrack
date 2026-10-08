package ar.edu.utn.donatrack.logistica;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RutaTest {

  // ── Helpers ────────────────────────────────────────────────────────

  private static Camion crearCamionDummy() {
    return new Camion("ABC123", 45.0, 2.5, 5000.0);
  }

  private static Destino crearDestino(long entidadId) {
    return new Destino(entidadId, "Calle " + entidadId, List.of(UUID.randomUUID()));
  }

  // ── TDD Cycle 1: Creation + Validation ──────────────────────────

  @Test
  void crearRuta_ConDatosValidos_DebeTenerAtributosCorrectos() {
    Camion camion = crearCamionDummy();
    Destino d1 = crearDestino(1);
    Destino d2 = crearDestino(2);
    Destino d3 = crearDestino(3);
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
    List<Destino> destinos = List.of();
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
    Destino repetido = new Destino(1L, "Calle A", List.of());
    Destino mismoEntidadOtraDireccion = new Destino(1L, "Calle B", List.of());
    List<Destino> destinos = List.of(repetido, mismoEntidadOtraDireccion);

    assertThrows(IllegalArgumentException.class, () -> new Ruta(camion, destinos));
  }

  @Test
  void crearRuta_ConDestinosDeDistintaEntidad_MantieneElOrden() {
    Camion camion = crearCamionDummy();
    Destino d7 = crearDestino(7);
    Destino d3 = crearDestino(3);
    Destino d5 = crearDestino(5);

    Ruta ruta = new Ruta(camion, List.of(d7, d3, d5));

    List<Destino> obtenidos = ruta.getDestinos();
    assertEquals(3, obtenidos.size());
    assertSame(d7, obtenidos.get(0));
    assertSame(d3, obtenidos.get(1));
    assertSame(d5, obtenidos.get(2));
  }

  @Test
  void getDestinos_AlModificarLista_DebeLanzarUnsupportedOperationException() {
    Camion camion = crearCamionDummy();
    List<Destino> destinos = List.of();
    Ruta ruta = new Ruta(camion, destinos);

    assertThrows(UnsupportedOperationException.class, () -> ruta.getDestinos().add(null));
  }

  // ── TDD Cycle 2: Lifecycle ────────────────────────────────────

  @Test
  void iniciar_CuandoNoEnCurso_DebeCambiarAtrue() {
    Ruta ruta = new Ruta(crearCamionDummy(), List.of());

    ruta.iniciar();

    assertTrue(ruta.isEnCurso());
  }

  @Test
  void iniciar_CuandoYaEnCurso_DebeLanzarIllegalStateException() {
    Ruta ruta = new Ruta(crearCamionDummy(), List.of());
    ruta.iniciar();

    assertThrows(IllegalStateException.class, ruta::iniciar);
    assertTrue(ruta.isEnCurso());
  }

  @Test
  void finalizar_CuandoEnCurso_DebeCambiarAfalse() {
    Ruta ruta = new Ruta(crearCamionDummy(), List.of());
    ruta.iniciar();

    ruta.finalizar();

    assertFalse(ruta.isEnCurso());
  }

  @Test
  void finalizar_CuandoNoEnCurso_DebeLanzarIllegalStateException() {
    Ruta ruta = new Ruta(crearCamionDummy(), List.of());

    assertThrows(IllegalStateException.class, ruta::finalizar);
    assertFalse(ruta.isEnCurso());
  }

  @Test
  void mismaUuid_EqualsDevuelveTrue() throws Exception {
    Ruta a = new Ruta(crearCamionDummy(), List.of());
    Ruta b = new Ruta(crearCamionDummy(), List.of());

    java.lang.reflect.Field field = Ruta.class.getDeclaredField("id");
    field.setAccessible(true);
    field.set(b, a.getId());

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void distintaUuid_EqualsDevuelveFalse() {
    Ruta a = new Ruta(crearCamionDummy(), List.of());
    Ruta b = new Ruta(crearCamionDummy(), List.of());

    assertNotEquals(a, b);
  }

  // ── TDD Cycle 3: Repository ────────────────────────────────────

  @Test
  void agregarRuta_LuegoBuscarPorId_DevuelveRuta() {
    RutaRepository repo = new RutaRepository();
    Ruta ruta = new Ruta(crearCamionDummy(), List.of());

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
    Ruta r1 = new Ruta(crearCamionDummy(), List.of());
    Ruta r2 = new Ruta(crearCamionDummy(), List.of());
    Ruta r3 = new Ruta(crearCamionDummy(), List.of());

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
