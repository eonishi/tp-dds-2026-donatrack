package ar.edu.utn.donatrack.schedules;

import ar.edu.utn.donatrack.app.ConfiguracionBase;
import ar.edu.utn.donatrack.donantes.DonantesRepository;
import ar.edu.utn.donatrack.notificaciones.BandejaNotificacionesRepository;
import ar.edu.utn.donatrack.notificaciones.NotificadorDesdeEntorno;
import ar.edu.utn.donatrack.notificaciones.ServicioNotificaciones;
import ar.edu.utn.donatrack.notificaciones.TareaAusenciaDonantes;
import java.time.Clock;

/**
 * Punto de entrada del aviso por ausencia de la plataforma para un planificador externo.
 *
 * <p>Es un programa autónomo: arma las dependencias de la tarea, notifica a las personas donantes
 * sin interacción en los últimos 20 días y termina. No corre dentro del proceso HTTP de donaciones;
 * un planificador externo (por ejemplo, un cronjob) lo invoca con la frecuencia que corresponda.
 */
public final class AusenciaDonantesRunner {
  public static void main(String[] args) {
    ConfiguracionBase.desdeEntorno();
    var notificaciones =
        new ServicioNotificaciones(
            NotificadorDesdeEntorno.crear(),
            new BandejaNotificacionesRepository(Clock.systemDefaultZone()));

    new TareaAusenciaDonantes(new DonantesRepository(), notificaciones).run();
    System.out.println("Aviso por ausencia de donantes ejecutado.");
  }
}
