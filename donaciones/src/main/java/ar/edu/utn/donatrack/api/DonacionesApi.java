package ar.edu.utn.donatrack.api;

import ar.edu.utn.donatrack.api.controller.DonacionController;
import ar.edu.utn.donatrack.api.controller.DonanteController;
import ar.edu.utn.donatrack.api.controller.EntidadController;
import ar.edu.utn.donatrack.api.controller.MatchmakingController;
import ar.edu.utn.donatrack.api.controller.NecesidadController;
import ar.edu.utn.donatrack.api.error.ApiExceptionHandler;
import ar.edu.utn.donatrack.beneficiarios.BeneficiariosRepository;
import ar.edu.utn.donatrack.donaciones.AlgoritmoCompatibilidadSemantica;
import ar.edu.utn.donatrack.donaciones.AlgoritmoPrioridadSubAtendidos;
import ar.edu.utn.donatrack.donaciones.CargaDonacion;
import ar.edu.utn.donatrack.donaciones.DonacionesRepository;
import ar.edu.utn.donatrack.donaciones.PropuestaRepository;
import ar.edu.utn.donatrack.donaciones.PropuestaRepositoryImpl;
import ar.edu.utn.donatrack.donaciones.ServicioMatchmakingDonaciones;
import ar.edu.utn.donatrack.donantes.DonantesRepository;
import ar.edu.utn.donatrack.notificaciones.NotificacionesRepository;
import ar.edu.utn.donatrack.notificaciones.NotificadorDesdeEntorno;
import ar.edu.utn.donatrack.notificaciones.ServicioNotificaciones;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.Javalin;
import java.time.Clock;
import java.util.List;

public final class DonacionesApi implements WithSimplePersistenceUnit {
  private DonacionesApi() {}

  public static Javalin create() {
    return create(Clock.systemUTC());
  }

  public static Javalin create(Clock clock) {
    return new DonacionesApi().build(clock);
  }

  private Javalin build(Clock clock) {
    DonacionesRepository donaciones = new DonacionesRepository();
    DonantesRepository donantes = new DonantesRepository();
    BeneficiariosRepository entidades = new BeneficiariosRepository();
    PropuestaRepository propuestas = new PropuestaRepositoryImpl();
    ServicioNotificaciones notificaciones =
        new ServicioNotificaciones(
            NotificadorDesdeEntorno.crear(), new NotificacionesRepository(), 3);

    DonanteController donanteController = new DonanteController(donantes, donaciones);
    EntidadController entidadController = new EntidadController(entidades, donaciones);
    NecesidadController necesidadController = new NecesidadController(entidades);
    DonacionController donacionController =
        new DonacionController(
            donaciones,
            donantes,
            propuestas,
            new CargaDonacion(java.time.LocalDate.now(clock), donaciones),
            notificaciones);

    ServicioMatchmakingDonaciones matchmakingDomain =
        new ServicioMatchmakingDonaciones(
            donaciones,
            entidades,
            propuestas,
            List.of(
                new AlgoritmoCompatibilidadSemantica(),
                new AlgoritmoPrioridadSubAtendidos(donaciones)));
    MatchmakingController matchmakingController =
        new MatchmakingController(matchmakingDomain, donaciones, entidades, notificaciones, clock);

    Javalin app =
        Javalin.create(
            config -> {
              config.showJavalinBanner = false;
              config.router.apiBuilder(
                  () ->
                      io.javalin.apibuilder.ApiBuilder.path(
                          "/api/v1",
                          () -> {
                            donanteController.routes();
                            entidadController.routes();
                            necesidadController.routes();
                            donacionController.routes();
                            matchmakingController.routes();
                          }));
            });

    java.util.concurrent.ScheduledExecutorService scheduler =
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleAtFixedRate(
        new ar.edu.utn.donatrack.notificaciones.TareaAusenciaDonantes(
            donantes, NotificadorDesdeEntorno.crear()),
        0,
        1,
        java.util.concurrent.TimeUnit.DAYS);
    app.events(
        event -> {
          event.serverStopping(() -> scheduler.shutdown());
        });

    ApiExceptionHandler.register(app, clock);
    // El contexto de persistencia es por hilo y la biblioteca no lo limpia: sin este reseteo, un
    // hilo reutilizado por Javalin serviría entidades cacheadas de una solicitud anterior y
    // devolvería lecturas obsoletas o rechazaría transiciones válidas. El reseteo es del contexto,
    // no de la transacción: cada caso de uso sigue siendo el único dueño de la suya.
    app.before(ctx -> entityManager().clear());
    return app;
  }
}
