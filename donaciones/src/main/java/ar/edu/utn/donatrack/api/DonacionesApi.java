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
import ar.edu.utn.donatrack.donaciones.DonacionesRepository;
import ar.edu.utn.donatrack.donaciones.PropuestaRepository;
import ar.edu.utn.donatrack.donaciones.PropuestaRepositoryImpl;
import ar.edu.utn.donatrack.donaciones.ServicioMatchmakingDonaciones;
import ar.edu.utn.donatrack.donantes.DonantesRepository;
import ar.edu.utn.donatrack.notificaciones.NotificadorDesdeEntorno;
import ar.edu.utn.donatrack.notificaciones.NotificacionesRepository;
import ar.edu.utn.donatrack.notificaciones.ServicioNotificaciones;
import io.javalin.Javalin;
import java.time.Clock;
import java.util.List;

public final class DonacionesApi {
  private DonacionesApi() {}

  public static Javalin create() {
    return create(Clock.systemUTC());
  }

  public static Javalin create(Clock clock) {
    DonacionesRepository donaciones = new DonacionesRepository();
    DonantesRepository donantes = new DonantesRepository();
    BeneficiariosRepository entidades = new BeneficiariosRepository();
    PropuestaRepository propuestas = new PropuestaRepositoryImpl();
    ServicioNotificaciones notificaciones =
        new ServicioNotificaciones(
            NotificadorDesdeEntorno.crear(), new NotificacionesRepository(), 3);

    DonanteController donanteController =
        new DonanteController(donantes, donaciones);
    EntidadController entidadController =
        new EntidadController(entidades, donaciones);
    NecesidadController necesidadController =
        new NecesidadController(entidades);
    DonacionController donacionController =
        new DonacionController(donaciones, donantes, entidades, propuestas);

    ServicioMatchmakingDonaciones matchmakingDomain =
        new ServicioMatchmakingDonaciones(
            donaciones,
            entidades,
            propuestas,
            List.of(
                new AlgoritmoCompatibilidadSemantica(),
                new AlgoritmoPrioridadSubAtendidos()));
    MatchmakingController matchmakingController =
        new MatchmakingController(matchmakingDomain, donaciones, entidades, notificaciones, clock);

    Javalin app =
        Javalin.create(
            config -> {
              config.showJavalinBanner = false;
              config.router.apiBuilder(
                  () -> {
                    donanteController.routes();
                    entidadController.routes("/api/v1/beneficiarias");
                    necesidadController.routes();
                    donacionController.routes();
                    matchmakingController.routes();
                  });
            });
    ApiExceptionHandler.register(app, clock);
    return app;
  }
}
