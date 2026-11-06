package ar.edu.utn.donatrack.testsupport;

import ar.edu.utn.donatrack.api.DonacionesApi;
import ar.edu.utn.donatrack.notificaciones.NotificacionesRepository;
import ar.edu.utn.donatrack.notificaciones.NotificadorSimulado;
import ar.edu.utn.donatrack.notificaciones.ServicioNotificaciones;
import ar.edu.utn.donatrack.seguimiento.PosicionesCamiones;
import io.javalin.Javalin;
import java.time.Clock;

/**
 * Arranca la API de donaciones para las pruebas HTTP con las notificaciones simuladas: el
 * notificador real exige las credenciales de Resend y Twilio, que las pruebas no tienen.
 */
public final class ApiDePrueba {
  private ApiDePrueba() {}

  public static Javalin crear() {
    return crear(Clock.systemUTC());
  }

  public static Javalin crear(Clock clock) {
    return DonacionesApi.create(clock, notificacionesSimuladas());
  }

  public static Javalin crear(Clock clock, PosicionesCamiones posicionesCamiones) {
    return DonacionesApi.create(clock, notificacionesSimuladas(), posicionesCamiones);
  }

  private static ServicioNotificaciones notificacionesSimuladas() {
    return new ServicioNotificaciones(new NotificadorSimulado(), new NotificacionesRepository(), 1);
  }
}
