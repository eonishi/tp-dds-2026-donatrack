package ar.edu.utn.donatrack.logistica.schedules;

import ar.edu.utn.donatrack.logistica.CamionRepository;
import ar.edu.utn.donatrack.logistica.ConfiguracionBase;
import ar.edu.utn.donatrack.logistica.RutaRepository;
import ar.edu.utn.donatrack.logistica.planificacion.DonacionesRemotasImpl;
import ar.edu.utn.donatrack.logistica.planificacion.PlanificadorRutas;
import ar.edu.utn.donatrack.logistica.planificacion.ServicioPlanificacionRutas;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.util.UUID;

/**
 * Punto de entrada de la planificación diaria de rutas para un planificador externo (enunciado 2,
 * requerimiento 5).
 *
 * <p>Es un programa autónomo: arma las dependencias del servicio de planificación, envía al
 * planificador de rutas las donaciones con asignación realizada y termina. No corre dentro del
 * proceso HTTP de logística; un planificador externo (por ejemplo, un cronjob) lo invoca en un
 * horario de baja carga.
 */
public final class PlanificacionRutasRunner {
  public static void main(String[] args) {
    ConfiguracionBase.desdeEntorno();
    String apiUrl = System.getenv().getOrDefault("DONACIONES_API_URL", "http://localhost:7070");
    String apiKey = System.getenv().getOrDefault("DONATRACK_API_KEY", "S2S_SECRET_KEY");
    // Igual que en MainLogistica: el id del lote lo devuelve el planificador externo (stub).
    PlanificadorRutas planificadorRutas = lote -> UUID.randomUUID();
    var servicioPlanificacion =
        new ServicioPlanificacionRutas(
            new DonacionesRemotasImpl(apiUrl, apiKey, HttpClient.newHttpClient(), new ObjectMapper()),
            new CamionRepository(),
            planificadorRutas,
            new RutaRepository());

    var lotes = servicioPlanificacion.planificar();
    System.out.println("Planificación de rutas ejecutada: " + lotes.size() + " lote(s) enviado(s).");
  }
}
