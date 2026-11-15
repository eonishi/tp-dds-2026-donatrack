package ar.edu.utn.donatrack.schedules;

import ar.edu.utn.donatrack.app.ConfiguracionBase;
import ar.edu.utn.donatrack.donantes.DonantesRepository;
import ar.edu.utn.donatrack.notificaciones.BandejaNotificacionesRepository;
import ar.edu.utn.donatrack.notificaciones.NotificadorDesdeEntorno;
import ar.edu.utn.donatrack.notificaciones.ServicioNotificaciones;
import ar.edu.utn.donatrack.notificaciones.eventos.EventoAusenciaDonante;
import java.time.Clock;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Punto de entrada del aviso por ausencia de la plataforma para un planificador externo.
 *
 * <p>Es un programa autónomo: notifica a las personas donantes sin interacción en los últimos 20
 * días y termina. No corre dentro del proceso HTTP de donaciones; un planificador externo (por
 * ejemplo, un cronjob) lo invoca con la frecuencia que corresponda.
 */
public final class AusenciaDonantesRunner {
  private static final Logger LOGGER = LoggerFactory.getLogger(AusenciaDonantesRunner.class);
  private static final int DIAS_SIN_INTERACCION = 20;

  public static void main(String[] args) {
    ConfiguracionBase.desdeEntorno();
    var notificaciones =
        new ServicioNotificaciones(
            NotificadorDesdeEntorno.crear(),
            new BandejaNotificacionesRepository(Clock.systemDefaultZone()));
    var fechaLimite = LocalDate.now().minusDays(DIAS_SIN_INTERACCION);

    new DonantesRepository()
        .getDonantesInactivos(fechaLimite)
        .forEach(donante -> notificaciones.publicar(new EventoAusenciaDonante(donante)));
    LOGGER.info("Aviso por ausencia de donantes ejecutado.");
  }
}
