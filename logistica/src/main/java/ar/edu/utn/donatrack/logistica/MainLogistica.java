package ar.edu.utn.donatrack.logistica;

import ar.edu.utn.donatrack.logistica.controllers.CamionController;
import ar.edu.utn.donatrack.logistica.controllers.PlanificacionController;
import ar.edu.utn.donatrack.logistica.controllers.PosicionController;
import ar.edu.utn.donatrack.logistica.controllers.RutaController;
import ar.edu.utn.donatrack.logistica.monitoreo.MonitorRutas;
import ar.edu.utn.donatrack.logistica.planificacion.DonacionesRemotas;
import ar.edu.utn.donatrack.logistica.planificacion.DonacionesRemotasImpl;
import ar.edu.utn.donatrack.logistica.planificacion.LoteId;
import ar.edu.utn.donatrack.logistica.planificacion.PlanificadorRutas;
import ar.edu.utn.donatrack.logistica.planificacion.ServicioPlanificacionRutas;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.Javalin;
import java.net.http.HttpClient;
import java.time.Clock;

public class MainLogistica implements WithSimplePersistenceUnit {
  public static void main(String[] args) {
    ConfiguracionBase.desdeEntorno();
    new MainLogistica().iniciar();
  }

  private void iniciar() {
    var app = Javalin.create().start(9001);

    // El contexto de persistencia es por hilo y la biblioteca no lo limpia: sin este reseteo, un
    // hilo reutilizado serviría entidades cacheadas de una solicitud anterior. El reseteo es del
    // contexto, no de la transacción: cada caso de uso sigue siendo el único dueño de la suya.
    app.before(ctx -> entityManager().clear());

    // 1. Instanciamos dependencias
    CamionRepository camionRepo = new CamionRepository();
    RutaRepository rutaRepo = new RutaRepository();
    MonitorRutas monitorRutas = new MonitorRutas(Clock.systemUTC(), rutaRepo);

    // 2. Instanciamos Controladores
    CamionController camionController = new CamionController(camionRepo);

    ObjectMapper objectMapper = new ObjectMapper();
    HttpClient httpClient = HttpClient.newHttpClient();
    String apiUrl = System.getenv().getOrDefault("DONACIONES_API_URL", "http://localhost:7070");
    String apiKey = System.getenv().getOrDefault("DONACIONES_API_KEY", "S2S_SECRET_KEY");
    DonacionesRemotas donacionesRemotas =
        new DonacionesRemotasImpl(apiUrl, apiKey, httpClient, objectMapper);

    RutaController rutaController = new RutaController(rutaRepo, camionRepo, donacionesRemotas);
    PosicionController posicionController =
        new PosicionController(monitorRutas, camionRepo, rutaRepo);

    PlanificadorRutas planificadorRutas = lote -> LoteId.generar();
    ServicioPlanificacionRutas servicioPlanificacion =
        new ServicioPlanificacionRutas(donacionesRemotas, camionRepo, planificadorRutas, rutaRepo);
    PlanificacionController planificacionController =
        new PlanificacionController(servicioPlanificacion, camionRepo);

    // 3. Registramos los Endpoints

    // --- Endpoints de Camiones ---
    app.get("/logistica/camiones", camionController::listar);
    app.get("/logistica/camiones/{patente}", camionController::buscar);
    app.post("/logistica/camiones", camionController::crear);
    app.put("/logistica/camiones/{patente}", camionController::actualizar);
    app.delete("/logistica/camiones/{patente}", camionController::eliminar);

    // --- Endpoints de Rutas ---
    app.get("/logistica/rutas", rutaController::listar);
    app.get("/logistica/rutas/{id}", rutaController::buscar);
    app.post("/logistica/rutas", rutaController::crear);
    app.patch("/logistica/rutas/{id}", rutaController::actualizar);
    app.delete("/logistica/rutas/{id}", rutaController::eliminar);

    // --- Endpoints de Posición (GPS) ---
    app.post("/logistica/camiones/{patente}/posicion", posicionController::ingestar);
    app.get("/logistica/camiones/{patente}/posicion", posicionController::leerPosicion);

    // --- Endpoints de Planificación ---
    app.post("/logistica/planificacion/callback", planificacionController::procesarResultado);

    System.out.println("Microservicio de Logística corriendo en http://localhost:9001");
  }
}
