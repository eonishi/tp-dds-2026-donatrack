package ar.edu.utn.donatrack.logistica;

import ar.edu.utn.donatrack.logistica.controllers.CamionController;
import ar.edu.utn.donatrack.logistica.controllers.PlanificacionController;
import ar.edu.utn.donatrack.logistica.controllers.PosicionController;
import ar.edu.utn.donatrack.logistica.controllers.RutaController;
import ar.edu.utn.donatrack.logistica.monitoreo.MonitorRutas;
import ar.edu.utn.donatrack.logistica.planificacion.*;
import io.javalin.Javalin;

import java.net.http.HttpClient;
import java.time.Clock;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MainLogistica {
  public static void main(String[] args) {
    var app = Javalin.create().start(9001);
    RequestPersistenceBoundary.register(app);

    // 1. Instanciamos dependencias
    CamionRepository camionRepo = new CamionRepository();
    RutaRepository rutaRepo = new RutaRepository();
    MonitorRutas monitorRutas = new MonitorRutas(Clock.systemUTC(), rutaRepo);

    // 2. Instanciamos Controladores
    CamionController camionController = new CamionController(camionRepo);

    ar.edu.utn.donatrack.logistica.planificacion.DonacionesRemotas donacionesRemotas =
        new ar.edu.utn.donatrack.logistica.planificacion.DonacionesRemotas() {
          public java.util.List<ar.edu.utn.donatrack.logistica.planificacion.DonacionPlanificable>
              listarEnAsignacionRealizada() {
            return java.util.List.of();
          }

          public void notificarListaParaEntregar(
              java.util.List<ar.edu.utn.donatrack.logistica.DonacionId> donaciones) {}

          public void notificarInicioTraslado(
              java.util.List<ar.edu.utn.donatrack.logistica.DonacionId> donaciones,
              String urlMapa) {}
        };
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
