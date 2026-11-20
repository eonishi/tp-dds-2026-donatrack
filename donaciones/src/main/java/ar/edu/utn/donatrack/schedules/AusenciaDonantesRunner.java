package ar.edu.utn.donatrack.schedules;

import ar.edu.utn.donatrack.app.ConfiguracionBase;
import ar.edu.utn.donatrack.donantes.DonantesRepository;
import ar.edu.utn.donatrack.notificaciones.BandejaNotificacionesRepository;
import ar.edu.utn.donatrack.notificaciones.NotificadorDesdeEntorno;
import ar.edu.utn.donatrack.notificaciones.ServicioNotificaciones;
import java.time.Clock;
import java.time.LocalDateTime;
import ar.edu.utn.donatrack.security.UsuariosRepository;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Punto de entrada del aviso por ausencia de la plataforma para un planificador externo.
 *
 * <p>Es un programa autónomo: notifica a las personas donantes sin interacción en los últimos 20
 * días y termina. No corre dentro del proceso HTTP de donaciones; un planificador externo (por
 * ejemplo, un cronjob) lo invoca con la frecuencia que corresponda.
 */
public final class AusenciaDonantesRunner implements WithSimplePersistenceUnit {
  private static final Logger LOGGER = LoggerFactory.getLogger(AusenciaDonantesRunner.class);
  private static final int DIAS_SIN_INTERACCION = 20;

  public static void main(String[] args) {
    ConfiguracionBase.desdeEntorno();
    var notificaciones =
        new ServicioNotificaciones(
            NotificadorDesdeEntorno.crear(),
            new BandejaNotificacionesRepository(Clock.systemDefaultZone()));
    new AusenciaDonantesRunner().ejecutar(notificaciones);
    LOGGER.info("Aviso por ausencia de donantes ejecutado.");
  }

  private void ejecutar(ServicioNotificaciones notificaciones) {
    var usuarios = new UsuariosRepository();
    var donantes = new DonantesRepository();
    var ahora = LocalDateTime.now();
    // Las cuentas anteriores a este campo comienzan a contarse desde su migración.
    withTransaction(() -> usuarios.donantesSinFechaRegistro()
        .forEach(u -> u.inicializarFechaRegistro(ahora)));
    for (var usuario : usuarios.donantesInactivos(ahora.minusDays(DIAS_SIN_INTERACCION))) {
      usuario.getDonanteId().flatMap(donantes::findById).ifPresent(donante -> {
        if (notificaciones.publicarAusencia(donante)) {
          withTransaction(() -> merge(usuario).registrarAvisoAusencia(ahora));
        }
      });
    }
  }
}
