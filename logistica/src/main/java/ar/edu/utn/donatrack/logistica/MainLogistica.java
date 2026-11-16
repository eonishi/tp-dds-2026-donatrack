package ar.edu.utn.donatrack.logistica;

import ar.edu.utn.donatrack.logistica.controllers.PlanificacionController;
import ar.edu.utn.donatrack.logistica.controllers.RepartidorController;
import ar.edu.utn.donatrack.logistica.demo.LogisticsDemoDataset;
import ar.edu.utn.donatrack.logistica.graphql.GraphqlController;
import ar.edu.utn.donatrack.logistica.graphql.GraphqlLogistica;
import ar.edu.utn.donatrack.logistica.graphql.ResolversLogistica;
import ar.edu.utn.donatrack.logistica.monitoreo.MonitorRutas;
import ar.edu.utn.donatrack.logistica.planificacion.DonacionesRemotas;
import ar.edu.utn.donatrack.logistica.planificacion.DonacionesRemotasImpl;
import ar.edu.utn.donatrack.logistica.planificacion.PlanificadorRutas;
import ar.edu.utn.donatrack.logistica.planificacion.ServicioPlanificacionRutas;
import ar.edu.utn.donatrack.logistica.security.LogisticaSecurity;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.Javalin;
import java.net.http.HttpClient;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainLogistica implements WithSimplePersistenceUnit {
  private static final Logger LOGGER = LoggerFactory.getLogger(MainLogistica.class);

  public static void main(String[] args) {
    ConfiguracionBase.desdeEntorno();
    new MainLogistica().iniciar();
  }

  private MonitorRutas monitorRutas;

  private void iniciar() {
    Javalin app = crearApp();
    new LogisticsDemoDataset().initialize(System.getenv(), monitorRutas, Clock.systemUTC());
    app.start(9001);
    LOGGER.info("Microservicio de Logística corriendo en http://localhost:9001");
  }

  public Javalin crearApp() {
    String webOrigin =
        System.getenv().getOrDefault("DONATRACK_WEB_ORIGIN", "http://localhost:7070");
    var app = Javalin.create();

    app.before(
        ctx -> {
          if (webOrigin.equals(ctx.header("Origin"))) {
            ctx.header("Access-Control-Allow-Origin", webOrigin);
            ctx.header("Access-Control-Allow-Credentials", "true");
            ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
            ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
          }
          if ("OPTIONS".equals(ctx.method().toString())) {
            ctx.status(204);
          }
        });
    app.options("/logistica/*", ctx -> ctx.status(204));

    // El contexto de persistencia es por hilo y la biblioteca no lo limpia: sin este reseteo, un
    // hilo reutilizado serviría entidades cacheadas de una solicitud anterior. El reseteo es del
    // contexto, no de la transacción: cada caso de uso sigue siendo el único dueño de la suya.
    app.before(ctx -> entityManager().clear());

    // 1. Instanciamos dependencias
    CamionRepository camionRepo = new CamionRepository();
    RutaRepository rutaRepo = new RutaRepository();
    RepartidorRepository repartidorRepo = new RepartidorRepository();
    cargarRepartidoresDePrueba(repartidorRepo);
    monitorRutas = new MonitorRutas(Clock.systemUTC(), rutaRepo);

    // 2. Instanciamos Controladores
    ObjectMapper objectMapper = new ObjectMapper();
    HttpClient httpClient = HttpClient.newHttpClient();
    String apiUrl = System.getenv().getOrDefault("DONACIONES_API_URL", "http://localhost:7070");
    // La misma clave interna que valida donaciones (ver .env.example).
    String apiKey = System.getenv().getOrDefault("DONATRACK_API_KEY", "S2S_SECRET_KEY");
    DonacionesRemotas donacionesRemotas =
        new DonacionesRemotasImpl(apiUrl, apiKey, httpClient, objectMapper);

    final RepartidorController repartidorController =
        new RepartidorController(repartidorRepo, Clock.systemUTC());
    // Camiones, rutas, repartidores y posiciones se exponen por GraphQL (entrega 4).
    GraphqlController graphqlController =
        new GraphqlController(
            GraphqlLogistica.crear(
                new ResolversLogistica(
                    camionRepo, rutaRepo, repartidorRepo, monitorRutas, donacionesRemotas)));

    // Este UUID es el id del lote que devuelve el planificador externo (stub), no la clave de una
    // entidad: el @GeneratedValue de las entidades no aplica acá.
    PlanificadorRutas planificadorRutas = lote -> UUID.randomUUID();
    // URL que se le pasa al planificador externo para que avise el resultado de cada lote.
    String urlCallback =
        System.getenv()
            .getOrDefault(
                "PLANIFICADOR_CALLBACK_URL",
                "http://localhost:9001/logistica/planificacion/callback");
    ServicioPlanificacionRutas servicioPlanificacion =
        new ServicioPlanificacionRutas(
            donacionesRemotas, camionRepo, planificadorRutas, rutaRepo, urlCallback);
    PlanificacionController planificacionController =
        new PlanificacionController(servicioPlanificacion, camionRepo);

    // 3. Registramos los Endpoints

    // El filtro valida las credenciales; los permisos de cada operación GraphQL los decide su
    // resolver. El login y el callback del planificador quedan afuera.
    app.before(LogisticaSecurity::autorizar);

    // --- Contrato GraphQL (camiones, rutas, repartidores y posiciones) ---
    app.post("/logistica/graphql", graphqlController::ejecutar);

    // --- Sesión del repartidor (HTTP) ---
    app.post("/logistica/login", repartidorController::login);
    app.post("/logistica/logout", repartidorController::logout);

    // --- Callback del planificador externo (HTTP: el contrato lo define el proveedor) ---
    app.post("/logistica/planificacion/callback", planificacionController::procesarResultado);

    return app;
  }

  // Mientras no haya alta de repartidores, se cargan usuarios de prueba al iniciar, igual que hace
  // donaciones con sus usuarios. Si ya están en la base no se tocan.
  private void cargarRepartidoresDePrueba(RepartidorRepository repartidorRepo) {
    withTransaction(
        () -> {
          for (String email :
              new String[] {"repartidor1@donatrack.local", "repartidor2@donatrack.local"}) {
            if (repartidorRepo.buscarPorEmail(email).isEmpty()) {
              repartidorRepo.agregar(
                  new Repartidor(email.substring(0, email.indexOf('@')), email, "repartidor123"));
            }
          }
        });
  }
}
