package ar.edu.utn.donatrack.testsupport;

import ar.edu.utn.donatrack.api.DonacionesApi;
import ar.edu.utn.donatrack.notificaciones.NotificadorPorCanal;
import ar.edu.utn.donatrack.notificaciones.ResultadoEnvio;
import ar.edu.utn.donatrack.notificaciones.ServicioNotificaciones;
import ar.edu.utn.donatrack.seguimiento.PosicionesCamiones;
import io.javalin.Javalin;
import java.time.Clock;

/**
 * Arranca la API de donaciones para las pruebas HTTP descartando las notificaciones: las pruebas no
 * deben contactar a Resend ni a Twilio.
 */
public final class ApiDePrueba {
  private ApiDePrueba() {}

  public static Javalin crear() {
    return crear(Clock.systemUTC());
  }

  public static Javalin crear(Clock clock) {
    return DonacionesApi.create(clock, notificacionesDescartadas());
  }

  public static Javalin crear(Clock clock, PosicionesCamiones posicionesCamiones) {
    return DonacionesApi.create(clock, notificacionesDescartadas(), posicionesCamiones);
  }

  private static ServicioNotificaciones notificacionesDescartadas() {
    return new ServicioNotificaciones(
        new NotificadorPorCanal(
            (destinatario, asunto, mensaje) -> ResultadoEnvio.aceptado(),
            (destinatario, mensaje) -> ResultadoEnvio.aceptado(),
            (destinatario, mensaje) -> ResultadoEnvio.aceptado()));
  }
}
