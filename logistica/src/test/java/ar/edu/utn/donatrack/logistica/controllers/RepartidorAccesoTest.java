package ar.edu.utn.donatrack.logistica.controllers;

import static ar.edu.utn.donatrack.logistica.graphql.GraphqlDePrueba.admin;
import static ar.edu.utn.donatrack.logistica.graphql.GraphqlDePrueba.pedir;
import static ar.edu.utn.donatrack.logistica.graphql.GraphqlDePrueba.repartidor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.donatrack.logistica.Camion;
import ar.edu.utn.donatrack.logistica.Repartidor;
import ar.edu.utn.donatrack.logistica.RepartidorRepository;
import ar.edu.utn.donatrack.logistica.Ruta;
import ar.edu.utn.donatrack.logistica.RutaRepository;
import ar.edu.utn.donatrack.logistica.graphql.GraphqlDePrueba;
import ar.edu.utn.donatrack.logistica.monitoreo.MonitorRutas;
import ar.edu.utn.donatrack.logistica.planificacion.DonacionesRemotas;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Sesión del repartidor y permisos sobre las rutas, con las operaciones por GraphQL. */
class RepartidorAccesoTest implements WithSimplePersistenceUnit {

  private final ObjectMapper mapper = new ObjectMapper();
  private RepartidorRepository repartidorRepo;
  private RutaRepository rutaRepo;
  private Repartidor chofer;
  private Repartidor acompaniante;
  private Repartidor otro;
  private Ruta ruta;
  private Javalin app;

  @BeforeEach
  void setUp() {
    repartidorRepo = new RepartidorRepository();
    chofer = new Repartidor("Chofer", "chofer@donatrack.local", "clave1");
    acompaniante = new Repartidor("Acompañante", "acomp@donatrack.local", "clave2");
    otro = new Repartidor("Otro", "otro@donatrack.local", "clave3");

    // La ruta y su camión se guardan en la base, igual que en producción.
    rutaRepo = new RutaRepository();
    Camion camion = new Camion("AB123CD", 20.0, 2.5, 10000.0);
    withTransaction(
        () -> {
          repartidorRepo.agregar(chofer);
          repartidorRepo.agregar(acompaniante);
          repartidorRepo.agregar(otro);
          persist(camion);
        });
    ruta = new Ruta(camion, List.of());
    ruta.asignarRepartidor(chofer);
    ruta.asignarRepartidor(acompaniante);
    withTransaction(() -> rutaRepo.agregar(ruta));

    app =
        GraphqlDePrueba.app(
            new ar.edu.utn.donatrack.logistica.CamionRepository(),
            rutaRepo,
            repartidorRepo,
            new MonitorRutas(Clock.systemUTC(), rutaRepo),
            Mockito.mock(DonacionesRemotas.class));
  }

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

  // Los resolvers leen y guardan la ruta en la base desde otro hilo: hay que volver a leerla.
  private Ruta rutaGuardada() {
    entityManager().clear();
    return rutaRepo.buscarPorId(ruta.getId()).orElseThrow();
  }

  private Map<String, Object> idRuta() {
    return Map.of("id", ruta.getId().toString());
  }

  @Test
  void elLoginConCredencialesValidasDevuelveElToken() {
    JavalinTest.test(
        app,
        (server, client) -> {
          var response =
              client.post(
                  "/logistica/login",
                  "{\"email\":\"Chofer@donatrack.local\",\"password\":\"clave1\"}");

          assertEquals(200, response.code());
          var body = mapper.readTree(response.body().string());
          assertEquals(chofer.getId().toString(), body.get("repartidorId").asText());
          assertFalse(body.get("token").asText().isBlank());
        });
  }

  @Test
  void elLoginConContraseniaIncorrectaDevuelve401() {
    JavalinTest.test(
        app,
        (server, client) ->
            assertEquals(
                401,
                client
                    .post(
                        "/logistica/login",
                        "{\"email\":\"chofer@donatrack.local\",\"password\":\"mal\"}")
                    .code()));
  }

  @Test
  void despuesDelLogoutElTokenDejaDeServir() {
    JavalinTest.test(
        app,
        (server, client) -> {
          String token = repartidor(chofer);
          assertEquals(200, pedir(client, token, "{ rutas { id } }").status());

          var logout =
              client.post("/logistica/logout", "", req -> req.header("Authorization", token));

          assertEquals(204, logout.code());
          assertEquals(401, pedir(client, token, "{ rutas { id } }").status());
        });
  }

  @Test
  void sinTokenNoSePuedeOperar() {
    JavalinTest.test(
        app,
        (server, client) -> assertEquals(401, pedir(client, null, "{ rutas { id } }").status()));
  }

  @Test
  void cualquierRepartidorAsignadoPuedeIniciarLaRuta() {
    JavalinTest.test(
        app,
        (server, client) -> {
          var respuesta =
              pedir(
                  client,
                  repartidor(acompaniante),
                  "mutation($id: ID!) { iniciarRuta(id: $id) { enCurso } }",
                  idRuta());

          assertNull(respuesta.codigoDeError());
          assertTrue(rutaGuardada().isEnCurso());
        });
  }

  @Test
  void unRepartidorNoAsignadoNoPuedeIniciarLaRuta() {
    JavalinTest.test(
        app,
        (server, client) -> {
          var respuesta =
              pedir(
                  client,
                  repartidor(otro),
                  "mutation($id: ID!) { iniciarRuta(id: $id) { enCurso } }",
                  idRuta());

          assertEquals("PROHIBIDO", respuesta.codigoDeError());
          assertFalse(rutaGuardada().isEnCurso());
        });
  }

  @Test
  void elRepartidorSoloVeSusRutas() {
    JavalinTest.test(
        app,
        (server, client) -> {
          var delChofer = pedir(client, repartidor(chofer), "{ rutas { id } }");
          var delOtro = pedir(client, repartidor(otro), "{ rutas { id } }");

          assertEquals(1, delChofer.data().path("rutas").size());
          assertEquals(0, delOtro.data().path("rutas").size());
        });
  }

  @Test
  void elRepartidorNoAdministraLaFlotaNiAsignaRepartidores() {
    JavalinTest.test(
        app,
        (server, client) -> {
          var camion =
              pedir(
                  client,
                  repartidor(chofer),
                  "mutation { crearCamion(input: {patente: \"ZZ999ZZ\", volumenM3: 1, alturaM: 1,"
                      + " capacidadCargaKg: 1}) { patente } }");
          var asignacion =
              pedir(
                  client,
                  repartidor(chofer),
                  "mutation($ruta: ID!, $rep: ID!) { asignarRepartidor(rutaId: $ruta, repartidorId:"
                      + " $rep) { id } }",
                  Map.of("ruta", ruta.getId().toString(), "rep", otro.getId().toString()));

          assertEquals("PROHIBIDO", camion.codigoDeError());
          assertEquals("PROHIBIDO", asignacion.codigoDeError());
          assertFalse(rutaGuardada().tieneAsignado(otro.getId()));
        });
  }

  @Test
  void elAdministradorAsignaVariosRepartidoresALaRuta() {
    JavalinTest.test(
        app,
        (server, client) -> {
          var respuesta =
              pedir(
                  client,
                  admin(),
                  "mutation($ruta: ID!, $rep: ID!) { asignarRepartidor(rutaId: $ruta, repartidorId:"
                      + " $rep) { repartidores } }",
                  Map.of("ruta", ruta.getId().toString(), "rep", otro.getId().toString()));

          assertEquals(3, respuesta.data().path("asignarRepartidor").path("repartidores").size());
          assertEquals(3, rutaGuardada().getRepartidores().size());
        });
  }
}
