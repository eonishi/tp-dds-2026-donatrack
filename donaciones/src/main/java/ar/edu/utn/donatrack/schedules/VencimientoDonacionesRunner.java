package ar.edu.utn.donatrack.schedules;

import ar.edu.utn.donatrack.app.ConfiguracionBase;
import ar.edu.utn.donatrack.donaciones.DonacionesRepository;
import ar.edu.utn.donatrack.propuestas.PropuestaRepositoryImpl;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Punto de entrada del control de vencimiento de donaciones para un planificador externo.
 *
 * <p>Es un programa autónomo: marca como vencidas las donaciones en depósito cuya fecha de
 * vencimiento ya pasó, borra sus propuestas de asignación pendientes y termina. No corre dentro del
 * proceso HTTP de donaciones; un planificador externo (por ejemplo, un cronjob) lo invoca con la
 * frecuencia que corresponda.
 */
public final class VencimientoDonacionesRunner implements WithSimplePersistenceUnit {
  private static final Logger LOGGER = LoggerFactory.getLogger(VencimientoDonacionesRunner.class);

  public static void main(String[] args) {
    ConfiguracionBase.desdeEntorno();
    var donacionesRepository = new DonacionesRepository();
    var propuestas = new PropuestaRepositoryImpl();
    var hoy = LocalDate.now();

    new VencimientoDonacionesRunner()
        .withTransaction(
            () ->
                donacionesRepository.getDonacionesEnDeposito().stream()
                    .filter(donacion -> donacion.estaVencida(hoy))
                    .forEach(
                        donacion -> {
                          donacion.marcarVencida();
                          propuestas.remove(donacion);
                        }));
    LOGGER.info("Control de vencimiento de donaciones ejecutado.");
  }
}
