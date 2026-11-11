package ar.edu.utn.donatrack.schedules;

import ar.edu.utn.donatrack.app.ConfiguracionBase;
import ar.edu.utn.donatrack.beneficiarios.BeneficiariosRepository;
import ar.edu.utn.donatrack.donaciones.DonacionesRepository;
import ar.edu.utn.donatrack.propuestas.PropuestaRepositoryImpl;
import ar.edu.utn.donatrack.propuestas.ServicioMatchmakingDonaciones;
import ar.edu.utn.donatrack.propuestas.matchmaking.AlgoritmoCompatibilidadSemantica;
import ar.edu.utn.donatrack.propuestas.matchmaking.AlgoritmoPrioridadSubAtendidos;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Punto de entrada del matchmaking diario para un planificador externo.
 *
 * <p>Es un programa autónomo: arma las dependencias del servicio de matchmaking, ejecuta la
 * generación de propuestas para la fecha actual y termina. No corre dentro del proceso HTTP de
 * donaciones; un planificador externo (por ejemplo, un cronjob) lo invoca con la frecuencia que
 * corresponda.
 */
public final class MatchmakingRunner implements WithSimplePersistenceUnit {
  private static final Logger LOGGER = LoggerFactory.getLogger(MatchmakingRunner.class);

  public static void main(String[] args) {
    ConfiguracionBase.desdeEntorno();
    var servicioMatchmaking =
        new ServicioMatchmakingDonaciones(
            new DonacionesRepository(),
            new BeneficiariosRepository(),
            new PropuestaRepositoryImpl(),
            List.of(new AlgoritmoCompatibilidadSemantica(), new AlgoritmoPrioridadSubAtendidos()));

    new MatchmakingRunner()
        .withTransaction(() -> servicioMatchmaking.ejecutarMatchmakingDiario(LocalDate.now()));
    LOGGER.info("Matchmaking diario ejecutado.");
  }
}
