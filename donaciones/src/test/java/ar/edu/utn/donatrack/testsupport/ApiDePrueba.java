package ar.edu.utn.donatrack.testsupport;

import ar.edu.utn.donatrack.api.DonacionesApi;
import ar.edu.utn.donatrack.api.ServiciosExternos;
import ar.edu.utn.donatrack.notificaciones.BandejaNotificacionesRepository;
import ar.edu.utn.donatrack.notificaciones.NotificacionesRepository;
import ar.edu.utn.donatrack.notificaciones.NotificadorSimulado;
import ar.edu.utn.donatrack.notificaciones.ServicioNotificaciones;
import ar.edu.utn.donatrack.seguimiento.PosicionesCamiones;
import ar.edu.utn.donatrack.ubicacion.Coordenadas;
import io.javalin.Javalin;
import java.time.Clock;
import java.util.Optional;

/**
 * Arranca la API de donaciones para las pruebas HTTP sin servicios externos: notificaciones
 * simuladas (el notificador real exige credenciales de Resend y Twilio), una flota en memoria en
 * lugar de logística y una geocodificación fija en lugar de Nominatim.
 */
public final class ApiDePrueba {
  /** Donde la geocodificación de prueba ubica cualquier dirección. */
  public static final Coordenadas UBICACION = new Coordenadas(-34.6037, -58.3816);

  private ApiDePrueba() {}

  public static Javalin crear() {
    return crear(Clock.systemUTC());
  }

  public static Javalin crear(Clock clock) {
    return crear(clock, patente -> Optional.empty());
  }

  public static Javalin crear(Clock clock, PosicionesCamiones posicionesCamiones) {
    return crear(clock, posicionesCamiones, new FlotaEnMemoria());
  }

  public static Javalin crear(
      Clock clock, PosicionesCamiones posicionesCamiones, FlotaEnMemoria flota) {
    return DonacionesApi.create(
        clock,
        notificacionesSimuladas(clock),
        new ServiciosExternos(posicionesCamiones, flota, direccion -> Optional.of(UBICACION)));
  }

  private static ServicioNotificaciones notificacionesSimuladas(Clock clock) {
    return new ServicioNotificaciones(
        new NotificadorSimulado(),
        new NotificacionesRepository(),
        1,
        new BandejaNotificacionesRepository(clock));
  }
}
