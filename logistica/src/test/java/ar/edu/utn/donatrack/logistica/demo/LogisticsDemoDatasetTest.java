package ar.edu.utn.donatrack.logistica.demo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ar.edu.utn.donatrack.logistica.*;
import ar.edu.utn.donatrack.logistica.graphql.GraphqlDePrueba;
import ar.edu.utn.donatrack.logistica.monitoreo.MonitorRutas;
import ar.edu.utn.donatrack.logistica.planificacion.DonacionesRemotas;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.testtools.JavalinTest;
import java.time.Clock;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LogisticsDemoDatasetTest implements WithSimplePersistenceUnit {
  private final CamionRepository camiones = new CamionRepository();
  private final RutaRepository rutas = new RutaRepository();
  private final RepartidorRepository repartidores = new RepartidorRepository();
  private final Clock clock = Clock.systemUTC();
  private final MonitorRutas monitor = new MonitorRutas(clock, rutas);
  private final LogisticsDemoDataset seed = new LogisticsDemoDataset();
  private static final Map<String, String> ENABLED = Map.of("DONATRACK_DEMO_DATA", "true");

  @AfterEach
  void cleanup() {
    entityManager().clear();
    withTransaction(
        () -> {
          rutas.listarTodas().forEach(this::remove);
          camiones.listarTodos().forEach(this::remove);
          repartidores.listarTodos().forEach(this::remove);
          createQuery("select v from LogisticsDemoVersion v", LogisticsDemoVersion.class)
              .getResultList()
              .forEach(this::remove);
        });
    entityManager().clear();
  }

  @Test
  void disabledByDefault() {
    seed.initialize(Map.of(), monitor, clock);
    assertTrue(camiones.listarTodos().isEmpty());
    assertTrue(rutas.listarTodas().isEmpty());
    assertNull(find(LogisticsDemoVersion.class, LogisticsDemoDataset.VERSION));
  }

  @Test
  void coversFleetRoutesDriversAndPositionWithoutDuplicatingOrResettingChanges() {
    seed.initialize(ENABLED, monitor, clock);
    assertEquals(3, camiones.listarTodos().size());
    assertEquals(2, rutas.listarTodas().size());
    assertEquals(2, repartidores.listarTodos().size());
    Ruta active = rutas.listarTodas().stream().filter(Ruta::isEnCurso).findFirst().orElseThrow();
    assertEquals(2, active.getRepartidores().size());
    assertTrue(active.getDestinos().isEmpty());
    assertTrue(monitor.ultimaPosicion(active.getCamion()).isPresent());
    assertTrue(
        repartidores
            .buscarPorEmail("demo.chofer@example.test")
            .orElseThrow()
            .tieneContrasenia("Demo12345!"));
    withTransaction(active::finalizar);
    seed.initialize(ENABLED, new MonitorRutas(clock, rutas), clock);
    assertEquals(3, camiones.listarTodos().size());
    assertEquals(2, rutas.listarTodas().size());
    assertFalse(rutas.buscarPorId(active.getId()).orElseThrow().isEnCurso());
  }

  @Test
  void rejectsCollisionsWithoutOverwritingOrAddingRecords() {
    withTransaction(() -> camiones.agregar(new Camion("DEMO-02", 99, 4, 9999)));
    assertThrows(IllegalStateException.class, () -> seed.initialize(ENABLED, monitor, clock));
    entityManager().clear();
    assertEquals(1, camiones.listarTodos().size());
    assertEquals(99, camiones.buscarPorPatente("DEMO-02").orElseThrow().getVolumenM3());
    assertTrue(rutas.listarTodas().isEmpty());
    assertTrue(repartidores.listarTodos().isEmpty());
    assertNull(find(LogisticsDemoVersion.class, LogisticsDemoDataset.VERSION));
  }

  @Test
  void graphqlExposesDemoDataWithoutRemoteCalls() {
    seed.initialize(ENABLED, monitor, clock);
    DonacionesRemotas remote = mock(DonacionesRemotas.class);
    var app = GraphqlDePrueba.app(camiones, rutas, repartidores, monitor, remote);
    JavalinTest.test(
        app,
        (server, client) -> {
          var response =
              GraphqlDePrueba.pedir(
                  client,
                  GraphqlDePrueba.admin(),
                  "{ camiones { patente } rutas { id enCurso destinos { entidadId } } "
                      + "posicion(patente: \"DEMO-01\") { latitud rutaActiva } }");
          assertNull(response.codigoDeError());
          assertEquals(3, response.data().path("camiones").size());
          assertEquals(2, response.data().path("rutas").size());
          assertEquals(-34.6037, response.data().path("posicion").path("latitud").asDouble());
        });
    verifyNoInteractions(remote);
  }
}
