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
 * planificador de rutas las donaciones con asignación realizada y termina. No guarda los lotes: el
 * resultado llega al callback del proceso HTTP de logística, que lo valida contra el estado actual
 * de las donaciones. No corre dentro del proceso HTTP de logística; un planificador externo (por
 * ejemplo, un cronjob) lo invoca en un horario de baja carga.
 */
public final class PlanificacionRutasRunner {
  public static void main(String[] args) {
    ConfiguracionBase.desdeEntorno();
    String apiUrl = System.getenv().getOrDefault("DONACIONES_API_URL", "http://localhost:7070");
    String apiKey = System.getenv().getOrDefault("DONATRACK_API_KEY", "S2S_SECRET_KEY");
    // Igual que en MainLogistica: el id del lote lo devuelve el planificador externo (stub).
    PlanificadorRutas planificadorRutas = lote -> UUID.randomUUID();
    // URL que se le pasa al planificador externo para que avise el resultado de cada lote.
    String urlCallback =
        System.getenv()
            .getOrDefault(
                "PLANIFICADOR_CALLBACK_URL",
                "http://localhost:9001/logistica/planificacion/callback");
    var servicioPlanificacion =
        new ServicioPlanificacionRutas(
            new DonacionesRemotasImpl(
                apiUrl, apiKey, HttpClient.newHttpClient(), new ObjectMapper()),
            new CamionRepository(),
            planificadorRutas,
            new RutaRepository(),
            urlCallback);

    var lote = servicioPlanificacion.planificar();
    System.out.println(
        "Planificación de rutas ejecutada: "
            + lote.map(id -> "lote " + id + " enviado.").orElse("no había nada para enviar."));
  }
}
