package ar.edu.utn.donatrack.logistica.schedules;

import ar.edu.utn.donatrack.logistica.CamionRepository;
import ar.edu.utn.donatrack.logistica.ConfiguracionBase;
import ar.edu.utn.donatrack.logistica.RutaRepository;
import ar.edu.utn.donatrack.logistica.planificacion.DonacionesRemotas;
import ar.edu.utn.donatrack.logistica.planificacion.DonacionesRemotasImpl;
import ar.edu.utn.donatrack.logistica.planificacion.PlanificadorRutas;
import ar.edu.utn.donatrack.logistica.planificacion.PlanificadorRutasHttp;
import ar.edu.utn.donatrack.logistica.planificacion.ServicioPlanificacionRutas;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private static final Logger LOGGER = LoggerFactory.getLogger(PlanificacionRutasRunner.class);

  public static void main(String[] args) {
    ConfiguracionBase.desdeEntorno();

    ObjectMapper objectMapper = new ObjectMapper();
    HttpClient httpClient = HttpClient.newHttpClient();

    String apiUrl = System.getenv().getOrDefault("DONACIONES_API_URL", "http://localhost:7070");
    String apiKey = System.getenv().getOrDefault("DONATRACK_API_KEY", "S2S_SECRET_KEY");
    DonacionesRemotas donacionesRemotas =
        new DonacionesRemotasImpl(apiUrl, apiKey, httpClient, objectMapper);

    String planificadorUrl =
        System.getenv().getOrDefault("PLANIFICADOR_RUTAS_URL", "http://localhost:9002");
    String planificadorApiKey =
        System.getenv().getOrDefault("PLANIFICADOR_RUTAS_API_KEY", "PLANIFICADOR_SECRET_KEY");
    PlanificadorRutas planificadorRutas =
        new PlanificadorRutasHttp(planificadorUrl, planificadorApiKey, httpClient, objectMapper);

    // URL que se le pasa al planificador externo para que avise el resultado de cada lote.
    String urlCallback =
        System.getenv()
            .getOrDefault(
                "PLANIFICADOR_CALLBACK_URL",
                "http://localhost:9001/logistica/planificacion/callback");
    var servicioPlanificacion =
        new ServicioPlanificacionRutas(
            donacionesRemotas,
            new CamionRepository(),
            planificadorRutas,
            new RutaRepository(),
            urlCallback);

    var lote = servicioPlanificacion.planificar();
    LOGGER.info(
        "Planificación de rutas ejecutada: {}",
        lote.map(id -> "lote " + id + " enviado.").orElse("no había nada para enviar."));
  }
}
