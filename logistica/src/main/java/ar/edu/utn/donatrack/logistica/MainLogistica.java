package ar.edu.utn.donatrack.logistica;

import ar.edu.utn.donatrack.logistica.controllers.CamionController;
import ar.edu.utn.donatrack.logistica.controllers.PosicionController; 
import ar.edu.utn.donatrack.logistica.controllers.RutaController;
import ar.edu.utn.donatrack.logistica.monitoreo.MonitorRutas; 
import io.javalin.Javalin;
import java.time.Clock; 

public class MainLogistica {
    public static void main(String[] args) {
        var app = Javalin.create().start(9001); 

        // 1. Instanciamos dependencias
        CamionRepository camionRepo = new CamionRepository();
        RutaRepository rutaRepo = new RutaRepository();
        MonitorRutas monitorRutas = new MonitorRutas(Clock.systemUTC(), rutaRepo); 
        
        // 2. Instanciamos Controladores
        CamionController camionController = new CamionController(camionRepo);
        RutaController rutaController = new RutaController(rutaRepo, camionRepo);
        PosicionController posicionController = new PosicionController(monitorRutas, camionRepo, rutaRepo); 

        // 3. Registramos los Endpoints
        
        // --- Endpoints de Camiones ---
        app.get("/api/logistica/camiones", camionController::listar);
        app.get("/api/logistica/camiones/{patente}", camionController::buscar);
        app.post("/api/logistica/camiones", camionController::crear);
        app.put("/api/logistica/camiones/{patente}", camionController::actualizar);
        app.delete("/api/logistica/camiones/{patente}", camionController::eliminar);

        // --- Endpoints de Rutas ---
        app.get("/api/logistica/rutas", rutaController::listar);
        app.get("/api/logistica/rutas/{id}", rutaController::buscar);
        app.post("/api/logistica/rutas", rutaController::crear);
        app.patch("/api/logistica/rutas/{id}/iniciar", rutaController::iniciar);
        app.patch("/api/logistica/rutas/{id}/finalizar", rutaController::finalizar);
        app.delete("/api/logistica/rutas/{id}", rutaController::eliminar);

        // --- Endpoints de Posición (GPS) ---  
        app.post("/api/logistica/camiones/{patente}/posicion", posicionController::ingestar);
        app.get("/api/logistica/camiones/{patente}/posicion", posicionController::leerPosicion);

        System.out.println("Microservicio de Logística corriendo en http://localhost:9001");
    }
}