package ar.edu.utn.donatrack.logistica.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.donatrack.logistica.Camion;
import ar.edu.utn.donatrack.logistica.Repartidor;
import ar.edu.utn.donatrack.logistica.RepartidorRepository;
import ar.edu.utn.donatrack.logistica.Ruta;
import ar.edu.utn.donatrack.logistica.RutaRepository;
import ar.edu.utn.donatrack.logistica.planificacion.DonacionesRemotas;
import ar.edu.utn.donatrack.logistica.security.LogisticaSecurity;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RepartidorAccesoTest implements WithSimplePersistenceUnit {

  // Los ids los asigna la base al persistir.
  private UUID idChofer;
  private UUID idAcompaniante;
  private UUID idOtro;

  private final ObjectMapper mapper = new ObjectMapper();
  private RepartidorRepository repartidorRepo;
  private RutaRepository rutaRepo;
  private Ruta ruta;
  private Javalin app;

  @BeforeEach
  void setUp() {
    repartidorRepo = new RepartidorRepository();
    Repartidor chofer = new Repartidor("Chofer", "chofer@donatrack.local", "clave1");
    Repartidor acompaniante = new Repartidor("Acompañante", "acomp@donatrack.local", "clave2");
    Repartidor otro = new Repartidor("Otro", "otro@donatrack.local", "clave3");
    withTransaction(
        () -> {
          repartidorRepo.agregar(chofer);
          repartidorRepo.agregar(acompaniante);
          repartidorRepo.agregar(otro);
        });
    idChofer = chofer.getId();
    idAcompaniante = acompaniante.getId();
    idOtro = otro.getId();

    // La ruta y su camión se guardan en la base, igual que en producción.
    rutaRepo = new RutaRepository();
    Camion camion = new Camion("AB123CD", 20.0, 2.5, 10000.0);
    ruta = new Ruta(camion, List.of());
    ruta.asignarRepartidor(chofer);
    ruta.asignarRepartidor(acompaniante);
    withTransaction(
        () -> {
          persist(camion);
          rutaRepo.agregar(ruta);
        });

    Clock clock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
    RepartidorController repartidores = new RepartidorController(repartidorRepo, rutaRepo, clock);
    RutaController rutas =
        new RutaController(rutaRepo, null, Mockito.mock(DonacionesRemotas.class));

    app =
        Javalin.create()
            .before(LogisticaSecurity::autorizar)
            .post("/logistica/login", repartidores::login)
            .post("/logistica/logout", repartidores::logout)
            .post("/logistica/rutas/{id}/repartidores", repartidores::asignarRuta)
            .get("/logistica/rutas", rutas::listar)
            .patch("/logistica/rutas/{id}", rutas::actualizar)
            // La gestión de la flota no se prueba acá: alcanza con ver que el filtro la ataje.
            .post("/logistica/camiones", ctx -> ctx.status(201))
            .get("/logistica/camiones/{patente}/posicion", ctx -> ctx.status(200));
  }

  // El login confirma sus lecturas fuera de toda transacción del test: se borran los repartidores
  // persistidos para que no choquen con el email único en la próxima prueba.
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

  // Los controladores leen y guardan la ruta en la base desde otro hilo: hay que volver a leerla.
  private Ruta rutaGuardada() {
    entityManager().clear();
    return rutaRepo.buscarPorId(ruta.getId()).orElseThrow();
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
          assertEquals(idChofer.toString(), body.get("repartidorId").asText());
          assertFalse(body.get("token").asText().isBlank());
        });
  }

  @Test
  void elLoginConContraseniaIncorrectaDevuelve401() {
    JavalinTest.test(
        app,
        (server, client) -> {
          var response =
              client.post(
                  "/logistica/login",
                  "{\"email\":\"chofer@donatrack.local\",\"password\":\"mal\"}");

          assertEquals(401, response.code());
        });
  }

  @Test
  void despuesDelLogoutElTokenDejaDeServir() {
    JavalinTest.test(
        app,
        (server, client) -> {
          String token = bearer(idChofer);
          assertEquals(
              200,
              client.get("/logistica/rutas", req -> req.header("Authorization", token)).code());

          var logout =
              client.post("/logistica/logout", "", req -> req.header("Authorization", token));

          assertEquals(204, logout.code());
          assertEquals(
              401,
              client.get("/logistica/rutas", req -> req.header("Authorization", token)).code());
        });
  }

  @Test
  void sinTokenNoSePuedeOperar() {
    JavalinTest.test(
        app, (server, client) -> assertEquals(401, client.get("/logistica/rutas").code()));
  }

  @Test
  void cualquierRepartidorAsignadoPuedeIniciarLaRuta() {
    JavalinTest.test(
        app,
        (server, client) -> {
          var response =
              client.patch(
                  "/logistica/rutas/" + ruta.getId(),
                  "{\"estado\":\"EN_CURSO\"}",
                  req -> req.header("Authorization", bearer(idAcompaniante)));

          assertEquals(200, response.code());
          assertTrue(rutaGuardada().isEnCurso());
        });
  }

  @Test
  void unRepartidorNoAsignadoNoPuedeIniciarLaRuta() {
    JavalinTest.test(
        app,
        (server, client) -> {
          var response =
              client.patch(
                  "/logistica/rutas/" + ruta.getId(),
                  "{\"estado\":\"EN_CURSO\"}",
                  req -> req.header("Authorization", bearer(idOtro)));

          assertEquals(403, response.code());
          assertFalse(rutaGuardada().isEnCurso());
        });
  }

  @Test
  void elRepartidorSoloVeSusRutas() {
    JavalinTest.test(
        app,
        (server, client) -> {
          var delChofer =
              client.get("/logistica/rutas", req -> req.header("Authorization", bearer(idChofer)));
          var delOtro =
              client.get("/logistica/rutas", req -> req.header("Authorization", bearer(idOtro)));

          assertEquals(1, mapper.readTree(delChofer.body().string()).size());
          assertEquals(0, mapper.readTree(delOtro.body().string()).size());
        });
  }

  @Test
  void elRepartidorNoAdministraLaFlotaNiAsignaRepartidores() {
    JavalinTest.test(
        app,
        (server, client) -> {
          var camion =
              client.post(
                  "/logistica/camiones",
                  "{}",
                  req -> req.header("Authorization", bearer(idChofer)));
          var asignacion =
              client.post(
                  "/logistica/rutas/" + ruta.getId() + "/repartidores",
                  "{\"repartidorId\":\"" + idOtro + "\"}",
                  req -> req.header("Authorization", bearer(idChofer)));

          assertEquals(403, camion.code());
          assertEquals(403, asignacion.code());
          assertFalse(rutaGuardada().tieneAsignado(idOtro));
        });
  }

  @Test
  void elAdministradorAsignaVariosRepartidoresALaRuta() {
    JavalinTest.test(
        app,
        (server, client) -> {
          String admin = "Bearer " + tokenDeAdministrador();
          var response =
              client.post(
                  "/logistica/rutas/" + ruta.getId() + "/repartidores",
                  "{\"repartidorId\":\"" + idOtro + "\"}",
                  req -> req.header("Authorization", admin));

          assertEquals(200, response.code());
          assertEquals(3, rutaGuardada().getRepartidores().size());
        });
  }

  @Test
  void donacionesConsultaLaPosicionConLaClaveInternaYNadaMas() {
    JavalinTest.test(
        app,
        (server, client) -> {
          var consulta =
              client.get(
                  "/logistica/camiones/AB123CD/posicion",
                  req -> req.header("Authorization", "clave-interna-de-prueba"));
          var otraOperacion =
              client.get(
                  "/logistica/rutas",
                  req -> req.header("Authorization", "clave-interna-de-prueba"));

          assertEquals(200, consulta.code());
          assertEquals(401, otraOperacion.code());
        });
  }

  private String bearer(UUID idRepartidor) {
    Repartidor repartidor = repartidorRepo.buscarPorId(idRepartidor).orElseThrow();
    return "Bearer " + LogisticaSecurity.emitirToken(repartidor, Instant.now());
  }

  // Simula el token que emite donaciones para una persona administradora, firmado con el secreto
  // compartido por defecto.
  private static String tokenDeAdministrador() throws Exception {
    Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
    String header = b64.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes());
    String payload =
        b64.encodeToString(
            ("{\"sub\":\"u-admin\",\"roles\":[\"ADMIN\"],\"exp\":"
                    + Instant.now().plusSeconds(600).getEpochSecond()
                    + "}")
                .getBytes(StandardCharsets.UTF_8));
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(
        new SecretKeySpec(
            "donatrack-dev-secret-change-me".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    String firma =
        b64.encodeToString(mac.doFinal((header + "." + payload).getBytes(StandardCharsets.UTF_8)));
    return header + "." + payload + "." + firma;
  }
}
