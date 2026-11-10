package ar.edu.utn.donatrack.api;

import ar.edu.utn.donatrack.api.controller.DonacionController;
import ar.edu.utn.donatrack.api.controller.DonanteController;
import ar.edu.utn.donatrack.api.controller.EntidadController;
import ar.edu.utn.donatrack.api.controller.MatchmakingController;
import ar.edu.utn.donatrack.api.controller.NecesidadController;
import ar.edu.utn.donatrack.api.controller.SubcategoriasController;
import ar.edu.utn.donatrack.api.controller.UsuarioController;
import ar.edu.utn.donatrack.api.error.ApiExceptionHandler;
import ar.edu.utn.donatrack.beneficiarios.BeneficiariosRepository;
import ar.edu.utn.donatrack.bienes.SubcategoriasRepository;
import ar.edu.utn.donatrack.donaciones.CargaDonacion;
import ar.edu.utn.donatrack.donaciones.DonacionesRepository;
import ar.edu.utn.donatrack.donantes.DonantesRepository;
import ar.edu.utn.donatrack.fotos.AlmacenFotos;
import ar.edu.utn.donatrack.fotos.AlmacenFotosDesdeEntorno;
import ar.edu.utn.donatrack.fotos.AlmacenFotosLocal;
import ar.edu.utn.donatrack.fotos.FotosRecepcion;
import ar.edu.utn.donatrack.importacion.ImportadorDonantes;
import ar.edu.utn.donatrack.notificaciones.BandejaNotificacionesRepository;
import ar.edu.utn.donatrack.notificaciones.NotificacionesRepository;
import ar.edu.utn.donatrack.notificaciones.NotificadorDesdeEntorno;
import ar.edu.utn.donatrack.notificaciones.ServicioNotificaciones;
import ar.edu.utn.donatrack.propuestas.PropuestaRepository;
import ar.edu.utn.donatrack.propuestas.PropuestaRepositoryImpl;
import ar.edu.utn.donatrack.propuestas.ServicioMatchmakingDonaciones;
import ar.edu.utn.donatrack.propuestas.matchmaking.AlgoritmoCompatibilidadSemantica;
import ar.edu.utn.donatrack.propuestas.matchmaking.AlgoritmoPrioridadSubAtendidos;
import ar.edu.utn.donatrack.security.AuthController;
import ar.edu.utn.donatrack.security.AuthService;
import ar.edu.utn.donatrack.security.ControlDeAcceso;
import ar.edu.utn.donatrack.seguimiento.PosicionesCamiones;
import ar.edu.utn.donatrack.seguimiento.PosicionesCamionesGraphql;
import ar.edu.utn.donatrack.ubicacion.UbicadorEntidades;
import ar.edu.utn.donatrack.web.HandlebarsRenderer;
import ar.edu.utn.donatrack.web.controller.ActivacionController;
import ar.edu.utn.donatrack.web.controller.AdminDonantesController;
import ar.edu.utn.donatrack.web.controller.AdminEntidadesController;
import ar.edu.utn.donatrack.web.controller.FotosLocalesController;
import ar.edu.utn.donatrack.web.controller.PaginasDeError;
import ar.edu.utn.donatrack.web.controller.PaginasPublicasController;
import ar.edu.utn.donatrack.web.controller.RecepcionesController;
import ar.edu.utn.donatrack.web.controller.RegistroController;
import ar.edu.utn.donatrack.web.controller.VistasPorRol;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.Javalin;
import io.javalin.config.SizeUnit;
import io.javalin.http.staticfiles.Location;
import java.time.Clock;
import java.util.List;

public final class DonacionesApi implements WithSimplePersistenceUnit {
  private DonacionesApi() {}

  public static Javalin create() {
    return create(Clock.systemUTC());
  }

  public static Javalin create(Clock clock) {
    return create(clock, notificacionesDesdeEntorno(), PosicionesCamionesGraphql.desdeEntorno());
  }

  public static Javalin create(Clock clock, ServicioNotificaciones notificaciones) {
    return create(clock, notificaciones, ServiciosExternos.desdeEntorno());
  }

  /** Permite reemplazar la consulta a logística, por ejemplo en las pruebas. */
  public static Javalin create(Clock clock, PosicionesCamiones posicionesCamiones) {
    return create(clock, notificacionesDesdeEntorno(), posicionesCamiones);
  }

  public static Javalin create(
      Clock clock, ServicioNotificaciones notificaciones, PosicionesCamiones posicionesCamiones) {
    return create(
        clock, notificaciones, ServiciosExternos.desdeEntorno().conPosiciones(posicionesCamiones));
  }

  public static Javalin create(
      Clock clock, ServicioNotificaciones notificaciones, ServiciosExternos externos) {
    return new DonacionesApi().build(clock, notificaciones, externos);
  }

  private static ServicioNotificaciones notificacionesDesdeEntorno() {
    return new ServicioNotificaciones(
        NotificadorDesdeEntorno.crear(),
        new NotificacionesRepository(),
        1,
        new BandejaNotificacionesRepository(Clock.systemDefaultZone()));
  }

  private Javalin build(
      Clock clock, ServicioNotificaciones notificaciones, ServiciosExternos externos) {
    PosicionesCamiones posicionesCamiones = externos.posiciones();
    DonacionesRepository donaciones = new DonacionesRepository();
    DonantesRepository donantes = new DonantesRepository();
    BeneficiariosRepository entidades = new BeneficiariosRepository();
    SubcategoriasRepository subcategorias = new SubcategoriasRepository();
    PropuestaRepository propuestas = new PropuestaRepositoryImpl();

    DonanteController donanteController =
        new DonanteController(donantes, donaciones, notificaciones);
    EntidadController entidadController = new EntidadController(entidades, donaciones);
    NecesidadController necesidadController = new NecesidadController(entidades, subcategorias);
    DonacionController donacionController =
        new DonacionController(
            donaciones,
            donantes,
            propuestas,
            new CargaDonacion(donaciones),
            notificaciones,
            subcategorias,
            posicionesCamiones);
    SubcategoriasController subcategoriasController = new SubcategoriasController(subcategorias);

    ServicioMatchmakingDonaciones matchmakingDomain =
        new ServicioMatchmakingDonaciones(
            donaciones,
            entidades,
            propuestas,
            List.of(new AlgoritmoCompatibilidadSemantica(), new AlgoritmoPrioridadSubAtendidos()));
    MatchmakingController matchmakingController =
        new MatchmakingController(matchmakingDomain, donaciones, entidades, notificaciones, clock);
    AuthService authService = new AuthService();
    AuthController authController = new AuthController(authService);
    UsuarioController usuarioController = new UsuarioController(authService, donantes, entidades);
    PaginasPublicasController paginasPublicas =
        new PaginasPublicasController(authService, donaciones);
    RegistroController registroController =
        new RegistroController(authService, donantes, entidades, notificaciones);
    AlmacenFotos almacenFotos = AlmacenFotosDesdeEntorno.crear();
    RecepcionesController recepciones =
        new RecepcionesController(donaciones, new FotosRecepcion(almacenFotos), notificaciones);
    ImportadorDonantes importador = new ImportadorDonantes(authService, clock);
    VistasPorRol vistasPorRol =
        new VistasPorRol(
            donaciones,
            entidades,
            subcategorias,
            propuestas,
            matchmakingDomain,
            notificaciones,
            new BandejaNotificacionesRepository(clock),
            posicionesCamiones,
            externos.flota(),
            importador,
            clock);
    ActivacionController activacion = new ActivacionController(authService, donantes);
    AdminEntidadesController adminEntidades = new AdminEntidadesController(authService, entidades);
    AdminDonantesController adminDonantes =
        new AdminDonantesController(
            authService,
            donantes,
            donaciones,
            new BandejaNotificacionesRepository(clock),
            notificaciones);

    Javalin app =
        Javalin.create(
            config -> {
              config.showJavalinBanner = false;
              // Vistas (MVC): las pantallas se renderizan acá con Handlebars sobre el mismo dominio
              // que expone la API; los estáticos (CSS, imágenes) salen de resources/public.
              config.fileRenderer(new HandlebarsRenderer());
              config.staticFiles.add("/public", Location.CLASSPATH);
              // Fotos de recepción: hasta 5 de 5 MB cada una en un mismo formulario.
              // El CSV de donantes puede pesar más (100.000 filas son unos 10 MB); el límite de 5
              // MB
              // por foto lo controla FotosRecepcion.
              config.jetty.multipartConfig.maxFileSize(20, SizeUnit.MB);
              config.jetty.multipartConfig.maxTotalRequestSize(30, SizeUnit.MB);
              config.jetty.multipartConfig.maxInMemoryFileSize(1, SizeUnit.MB);
              config.router.apiBuilder(
                  () -> {
                    io.javalin.apibuilder.ApiBuilder.path(
                        "/api/v1",
                        () -> {
                          donanteController.routes();
                          entidadController.routes();
                          necesidadController.routes();
                          donacionController.routes();
                          subcategoriasController.routes();
                          matchmakingController.routes();
                          usuarioController.routes();
                        });
                    authController.routes();
                    paginasPublicas.routes();
                    registroController.routes();
                    recepciones.routes();
                    vistasPorRol.routes();
                    activacion.routes();
                    adminDonantes.routes();
                    adminEntidades.routes();
                    if (almacenFotos instanceof AlmacenFotosLocal local) {
                      new FotosLocalesController(local).routes();
                    }
                  });
            });

    // Permisos: cada ruta declara sus roles al registrarse y este filtro los hace cumplir.
    app.beforeMatched(ctx -> ControlDeAcceso.verificar(ctx, authService));

    java.util.concurrent.ScheduledExecutorService scheduler =
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleAtFixedRate(
        new ar.edu.utn.donatrack.notificaciones.TareaAusenciaDonantes(donantes, notificaciones),
        0,
        1,
        java.util.concurrent.TimeUnit.DAYS);
    // Ubica en el mapa las entidades nuevas o con dirección cambiada (un pedido por segundo).
    UbicadorEntidades ubicador = new UbicadorEntidades(externos.geocodificador(), 1100);
    scheduler.scheduleWithFixedDelay(
        () -> {
          try {
            ubicador.ubicarPendientes();
          } catch (RuntimeException e) {
            java.util.logging.Logger.getLogger(DonacionesApi.class.getName())
                .log(java.util.logging.Level.WARNING, "Falló la ubicación de entidades", e);
          }
        },
        10,
        60,
        java.util.concurrent.TimeUnit.SECONDS);
    app.events(
        event -> {
          event.serverStopping(
              () -> {
                scheduler.shutdownNow();
                importador.detener();
              });
        });

    ApiExceptionHandler.register(app, clock);
    PaginasDeError.register(app, authService);
    // El contexto de persistencia es por hilo y la biblioteca no lo limpia: sin este reseteo, un
    // hilo reutilizado por Javalin serviría entidades cacheadas de una solicitud anterior y
    // devolvería lecturas obsoletas o rechazaría transiciones válidas. El reseteo es del contexto,
    // no de la transacción: cada caso de uso sigue siendo el único dueño de la suya.
    app.before(ctx -> entityManager().clear());
    return app;
  }
}
