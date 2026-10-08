package ar.edu.utn.donatrack.monitoreo;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.donatrack.logistica.Camion;
import ar.edu.utn.donatrack.logistica.Destino;
import ar.edu.utn.donatrack.logistica.Ruta;
import ar.edu.utn.donatrack.logistica.RutaRepository;
import ar.edu.utn.donatrack.logistica.monitoreo.Coordenada;
import ar.edu.utn.donatrack.logistica.monitoreo.LecturaPosicion;
import ar.edu.utn.donatrack.logistica.monitoreo.MonitorRutas;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MonitoreoTest implements SimplePersistenceTest {

  // ── Helpers ────────────────────────────────────────────────────────

  private static Camion crearCamion(String patente) {
    return new Camion(patente, 45.0, 2.5, 5000.0);
  }

  /** Persiste el camión y su ruta activa para que el monitor la encuentre en la base. */
  private Ruta almacenarRutaActiva(Camion camion) {
    Ruta ruta = new Ruta(camion, List.of(new Destino(1L, "Calle A", List.of())));
    persist(camion);
    ruta.iniciar();
    new RutaRepository().agregar(ruta);
    return ruta;
  }

  private static Coordenada coordValida() {
    return new Coordenada(-34.6037, -58.3816);
  }

  // ── Phase 1: Coordenada (4 scenarios) ─────────────────────────────

  @Test
  void crearCoordenada_ConValoresValidos_DebeTenerLatitudYLongitudCorrectas() {
    Coordenada coord = coordValida();

    assertEquals(-34.6037, coord.latitud(), 0.0001);
    assertEquals(-58.3816, coord.longitud(), 0.0001);
  }

  @Test
  void crearCoordenada_LatitudFueraDeRango_DebeLanzarIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> new Coordenada(100, 0));
  }

  @Test
  void crearCoordenada_LongitudFueraDeRango_DebeLanzarIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> new Coordenada(0, 200));
  }

  @Test
  void crearCoordenada_ConNaN_DebeLanzarIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> new Coordenada(Double.NaN, 0));
  }

  // ── Phase 1: LecturaPosicion (2 scenarios) ───────────────────────

  @Test
  void crearLecturaPosicion_ConDatosValidos_DebeTenerTodosLosCampos() {
    Camion camion = crearCamion("ABC123");
    Coordenada coord = coordValida();
    LocalDateTime now = LocalDateTime.of(2026, 7, 12, 15, 30, 0);

    LecturaPosicion lectura = new LecturaPosicion(camion, coord, 45.0, now);

    assertAll(
        () -> assertSame(camion, lectura.camion()),
        () -> assertSame(coord, lectura.coordenada()),
        () -> assertEquals(45.0, lectura.velocidad(), 0.001),
        () -> assertEquals(now, lectura.timestamp()));
  }

  @Test
  void crearLecturaPosicion_ConCamionNull_DebeLanzarNullPointerException() {
    Coordenada coord = coordValida();

    assertThrows(
        NullPointerException.class,
        () -> new LecturaPosicion(null, coord, 45.0, LocalDateTime.now()));
  }

  // ── Phase 2: RutaRepository.buscarPorCamion (2 scenarios) ──────

  @Test
  void buscarPorCamion_ConRutaActiva_DebeRetornarOptionalConRuta() {
    Camion camion = crearCamion("ABC123");
    Ruta ruta = almacenarRutaActiva(camion);
    RutaRepository repo = new RutaRepository();

    Optional<Ruta> resultado = repo.buscarPorCamion(camion);

    assertTrue(resultado.isPresent());
    assertSame(ruta, resultado.get());
  }

  @Test
  void buscarPorCamion_SinRutaActiva_DebeRetornarOptionalEmpty() {
    Camion camion = crearCamion("ABC123");
    persist(camion);
    RutaRepository repo = new RutaRepository();

    Optional<Ruta> resultado = repo.buscarPorCamion(camion);

    assertTrue(resultado.isEmpty());
  }

  // ── Phase 3: MonitorRutas.procesar (3 scenarios) ──────────────

  @Test
  void procesar_LecturaValidaConRutaActiva_DebeActualizarUltimaPosicion() {
    Camion camion = crearCamion("ABC123");
    almacenarRutaActiva(camion);
    RutaRepository repo = new RutaRepository();

    Instant now = Instant.parse("2026-07-12T15:30:00Z");
    Clock clock = Clock.fixed(now, ZoneId.of("UTC"));
    MonitorRutas monitor = new MonitorRutas(clock, repo);

    LocalDateTime timestamp = LocalDateTime.of(2026, 7, 12, 15, 0, 0);
    Coordenada coord = coordValida();
    LecturaPosicion lectura = new LecturaPosicion(camion, coord, 45.0, timestamp);

    monitor.procesar(lectura);

    Optional<LecturaPosicion> ultima = monitor.ultimaPosicion(camion);
    assertTrue(ultima.isPresent());
    assertSame(lectura, ultima.get());
  }

  @Test
  void procesar_TimestampFuturo_DebeRechazarYNoActualizarUltimaPosicion() {
    Camion camion = crearCamion("ABC123");
    almacenarRutaActiva(camion);
    RutaRepository repo = new RutaRepository();

    Instant now = Instant.parse("2026-07-12T15:00:00Z");
    Clock clock = Clock.fixed(now, ZoneId.of("UTC"));
    MonitorRutas monitor = new MonitorRutas(clock, repo);

    LocalDateTime timestampFuturo = LocalDateTime.of(2026, 7, 12, 16, 0, 0);
    Coordenada coord = coordValida();
    LecturaPosicion lectura = new LecturaPosicion(camion, coord, 45.0, timestampFuturo);

    monitor.procesar(lectura);

    Optional<LecturaPosicion> ultima = monitor.ultimaPosicion(camion);
    assertTrue(ultima.isEmpty());
  }

  @Test
  void procesar_CamionSinRutaActiva_DebeRechazarYNoActualizarUltimaPosicion() {
    Camion camion = crearCamion("ABC123");
    persist(camion);
    RutaRepository repo = new RutaRepository();

    Clock clock = Clock.systemUTC();
    MonitorRutas monitor = new MonitorRutas(clock, repo);

    LocalDateTime timestamp = LocalDateTime.of(2026, 7, 12, 15, 0, 0);
    Coordenada coord = coordValida();
    LecturaPosicion lectura = new LecturaPosicion(camion, coord, 45.0, timestamp);

    monitor.procesar(lectura);

    Optional<LecturaPosicion> ultima = monitor.ultimaPosicion(camion);
    assertTrue(ultima.isEmpty());
  }

  // ── Phase 3: MonitorRutas.ultimaPosicion (2 scenarios) ─────────

  @Test
  void ultimaPosicion_ConLecturasPrevias_DebeRetornarOptionalConUltimaLectura() {
    Camion camion = crearCamion("ABC123");
    almacenarRutaActiva(camion);
    RutaRepository repo = new RutaRepository();

    Instant now = Instant.parse("2026-07-12T15:30:00Z");
    Clock clock = Clock.fixed(now, ZoneId.of("UTC"));
    MonitorRutas monitor = new MonitorRutas(clock, repo);

    LocalDateTime ts1 = LocalDateTime.of(2026, 7, 12, 15, 0, 0);
    LocalDateTime ts2 = LocalDateTime.of(2026, 7, 12, 15, 10, 0);
    LecturaPosicion lectura1 = new LecturaPosicion(camion, coordValida(), 40.0, ts1);
    LecturaPosicion lectura2 = new LecturaPosicion(camion, coordValida(), 50.0, ts2);

    monitor.procesar(lectura1);
    monitor.procesar(lectura2);

    Optional<LecturaPosicion> ultima = monitor.ultimaPosicion(camion);
    assertTrue(ultima.isPresent());
    assertSame(lectura2, ultima.get());
  }

  @Test
  void ultimaPosicion_SinReportes_DebeRetornarOptionalEmpty() {
    Camion camion = crearCamion("ABC123");
    RutaRepository repo = new RutaRepository();
    Clock clock = Clock.systemUTC();
    MonitorRutas monitor = new MonitorRutas(clock, repo);

    Optional<LecturaPosicion> ultima = monitor.ultimaPosicion(camion);

    assertTrue(ultima.isEmpty());
  }
}
