package ar.edu.utn.donatrack.logistica.graphql;

import static ar.edu.utn.donatrack.logistica.graphql.GraphqlDePrueba.admin;
import static ar.edu.utn.donatrack.logistica.graphql.GraphqlDePrueba.pedir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ar.edu.utn.donatrack.logistica.Camion;
import ar.edu.utn.donatrack.logistica.CamionRepository;
import ar.edu.utn.donatrack.logistica.Destino;
import ar.edu.utn.donatrack.logistica.Repartidor;
import ar.edu.utn.donatrack.logistica.RepartidorRepository;
import ar.edu.utn.donatrack.logistica.Ruta;
import ar.edu.utn.donatrack.logistica.RutaRepository;
import ar.edu.utn.donatrack.logistica.monitoreo.MonitorRutas;
import ar.edu.utn.donatrack.logistica.planificacion.DonacionesRemotas;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Las operaciones que antes eran endpoints REST de camiones, rutas y posiciones, ahora por GraphQL,
 * con el mismo comportamiento.
 */
class GraphqlLogisticaTest implements WithSimplePersistenceUnit {

  private CamionRepository camiones;
  private RutaRepository rutas;
  private DonacionesRemotas donacionesRemotas;
  private Javalin app;

  @BeforeEach
  void setUp() {
    camiones = new CamionRepository();
    rutas = new RutaRepository();
    donacionesRemotas = mock(DonacionesRemotas.class);
    app =
        GraphqlDePrueba.app(
            camiones,
            rutas,
            new RepartidorRepository(),
            new MonitorRutas(Clock.systemUTC(), rutas),
            donacionesRemotas);
  }

  // Cada pedido confirma su propia transacción: se borra lo que quedó para no contaminar las demás.
  @AfterEach
  void limpiarPersistencia() {
    entityManager().clear();
    withTransaction(
        () -> {
          createQuery("select r from Ruta r", Ruta.class).getResultList().forEach(this::remove);
          createQuery("select c from Camion c", Camion.class).getResultList().forEach(this::remove);
          createQuery("select r from Repartidor r", Repartidor.class)
              .getResultList()
              .forEach(this::remove);
        });
    entityManager().clear();
  }

  @Test
  void crearCamionLoGuardaEnElRepositorio() {
    JavalinTest.test(
        app,
        (server, client) -> {
          var respuesta =
              pedir(
                  client,
                  admin(),
                  "mutation { crearCamion(input: {patente: \"AF123ZZ\", volumenM3: 20.5, alturaM: 2.5,"
                      + " capacidadCargaKg: 15000}) { patente } }");

          assertEquals(200, respuesta.status());
          assertEquals("AF123ZZ", respuesta.data().path("crearCamion").path("patente").asText());
          Camion guardado = camiones.buscarPorPatente("AF123ZZ").orElseThrow();
          assertEquals(20.5, guardado.getVolumenM3());
          assertEquals(2.5, guardado.getAlturaM());
          assertEquals(15000.0, guardado.getCapacidadCargaKg());
        });
  }

  @Test
  void listarCamionesAlIniciarDevuelveUnaListaVacia() {
    JavalinTest.test(
        app,
        (server, client) -> {
          var respuesta = pedir(client, admin(), "{ camiones { patente } }");

          assertEquals(200, respuesta.status());
          assertEquals(0, respuesta.data().path("camiones").size());
        });
  }

  @Test
  void actualizarCamionCambiaSusDatos() {
    guardarCamion("AF123ZZ");
    JavalinTest.test(
        app,
        (server, client) -> {
          var respuesta =
              pedir(
                  client,
                  admin(),
                  "mutation { actualizarCamion(input: {patente: \"AF123ZZ\", volumenM3: 30,"
                      + " alturaM: 3, capacidadCargaKg: 20000}) { volumenM3 } }");

          assertEquals(
              30.0, respuesta.data().path("actualizarCamion").path("volumenM3").asDouble());
          entityManager().clear();
          assertEquals(30.0, camiones.buscarPorPatente("AF123ZZ").orElseThrow().getVolumenM3());
        });
  }

  @Test
  void crearRutaConCamionExistenteLaGuarda() {
    guardarCamion("AB123CD");
    JavalinTest.test(
        app,
        (server, client) -> {
          var respuesta =
              pedir(
                  client,
                  admin(),
                  "mutation { crearRuta(input: {patenteCamion: \"AB123CD\", destinos: []}) {"
                      + " camion { patente } enCurso } }");

          var ruta = respuesta.data().path("crearRuta");
          assertEquals("AB123CD", ruta.path("camion").path("patente").asText());
          assertFalse(ruta.path("enCurso").asBoolean());
          assertEquals(1, rutas.listarTodas().size(), "El repositorio debería tener 1 ruta");
        });
  }

  @Test
  void crearRutaConCamionInexistenteDevuelveDatosInvalidos() {
    JavalinTest.test(
        app,
        (server, client) -> {
          var respuesta =
              pedir(
                  client,
                  admin(),
                  "mutation { crearRuta(input: {patenteCamion: \"FALSO99\", destinos: []}) { id } }");

          assertEquals("DATOS_INVALIDOS", respuesta.codigoDeError());
          assertEquals("No se puede crear la ruta: Camión inexistente", respuesta.mensajeDeError());
          assertEquals(0, rutas.listarTodas().size(), "No se debería haber guardado ninguna ruta");
        });
  }

  @Test
  void eliminarRutaExistenteLaQuitaDeLaBase() {
    Ruta ruta = guardarRuta(UUID.randomUUID());
    JavalinTest.test(
        app,
        (server, client) -> {
          var respuesta =
              pedir(
                  client,
                  admin(),
                  "mutation($id: ID!) { eliminarRuta(id: $id) }",
                  Map.of("id", ruta.getId().toString()));

          assertTrue(respuesta.data().path("eliminarRuta").asBoolean());
          entityManager().clear();
          assertTrue(rutas.buscarPorId(ruta.getId()).isEmpty());
        });
  }

  @Test
  void iniciarRutaPersisteElEstadoYNotificaLaPatenteUnaSolaVez() {
    UUID donacion = UUID.randomUUID();
    Ruta ruta = guardarRuta(donacion);
    JavalinTest.test(
        app,
        (server, client) -> {
          var respuesta =
              pedir(
                  client,
                  admin(),
                  "mutation($id: ID!) { iniciarRuta(id: $id) { enCurso } }",
                  Map.of("id", ruta.getId().toString()));

          assertTrue(respuesta.data().path("iniciarRuta").path("enCurso").asBoolean());
          entityManager().clear();
          assertTrue(rutas.buscarPorId(ruta.getId()).orElseThrow().isEnCurso());
          verify(donacionesRemotas).notificarInicioTraslado(List.of(donacion), "AB123CD");
        });
  }

  @Test
  void laPosicionReportadaSeConsultaConLaRutaActiva() {
    Ruta ruta = guardarRuta(UUID.randomUUID());
    JavalinTest.test(
        app,
        (server, client) -> {
          pedir(
              client,
              admin(),
              "mutation($id: ID!) { iniciarRuta(id: $id) { id } }",
              Map.of("id", ruta.getId().toString()));
          var reporte =
              pedir(
                  client,
                  admin(),
                  "mutation { reportarPosicion(input: {patente: \"AB123CD\", latitud: -34.665,"
                      + " longitud: -58.37, velocidad: 45.5, timestamp: \"2026-09-09T18:15:00\"}) }");
          var consulta =
              pedir(
                  client,
                  admin(),
                  "{ posicion(patente: \"AB123CD\") { patente rutaActiva latitud longitud"
                      + " actualizadaEn } }");

          assertTrue(reporte.data().path("reportarPosicion").asBoolean());
          var posicion = consulta.data().path("posicion");
          assertEquals("AB123CD", posicion.path("patente").asText());
          assertEquals(ruta.getId().toString(), posicion.path("rutaActiva").asText());
          assertEquals(-34.665, posicion.path("latitud").asDouble());
          assertEquals(-58.37, posicion.path("longitud").asDouble());
          assertEquals("2026-09-09T18:15", posicion.path("actualizadaEn").asText());
        });
  }

  @Test
  void unCamionSinLecturasNoTienePosicion() {
    guardarCamion("AB123CD");
    JavalinTest.test(
        app,
        (server, client) -> {
          var respuesta = pedir(client, admin(), "{ posicion(patente: \"AB123CD\") { latitud } }");

          assertNull(respuesta.codigoDeError());
          assertTrue(respuesta.data().path("posicion").isNull());
        });
  }

  @Test
  void donacionesConLaClaveInternaSoloConsultaPosiciones() {
    guardarCamion("AB123CD");
    JavalinTest.test(
        app,
        (server, client) -> {
          String clave = GraphqlDePrueba.CLAVE_INTERNA;
          var posicion = pedir(client, clave, "{ posicion(patente: \"AB123CD\") { latitud } }");
          var flota = pedir(client, clave, "{ camiones { patente } }");

          assertNull(posicion.codigoDeError());
          assertEquals("PROHIBIDO", flota.codigoDeError());
        });
  }

  @Test
  void sinCredencialesResponde401() {
    JavalinTest.test(
        app,
        (server, client) ->
            assertEquals(401, pedir(client, null, "{ camiones { patente } }").status()));
  }

  private Camion guardarCamion(String patente) {
    Camion camion = new Camion(patente, 20.0, 2.5, 10000.0);
    withTransaction(() -> camiones.agregar(camion));
    return camion;
  }

  private Ruta guardarRuta(UUID donacion) {
    Camion camion = guardarCamion("AB123CD");
    Ruta ruta = new Ruta(camion, List.of(new Destino(1L, "Calle 123", List.of(donacion))));
    withTransaction(() -> rutas.agregar(ruta));
    return ruta;
  }
}
