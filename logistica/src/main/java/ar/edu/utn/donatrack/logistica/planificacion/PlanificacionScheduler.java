package ar.edu.utn.donatrack.logistica.planificacion;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Corre la planificación de rutas una vez por día en un horario de baja carga (enunciado 2,
 * requerimiento 5). La hora se configura con {@code PLANIFICACION_HORA} (0 a 23, por defecto las 3)
 * y se interpreta en la zona horaria del reloj, no en la del servidor.
 */
public class PlanificacionScheduler {
  private static final Logger LOG = Logger.getLogger(PlanificacionScheduler.class.getName());
  private static final LocalTime HORA_POR_DEFECTO = LocalTime.of(3, 0);

  private final ServicioPlanificacionRutas servicioPlanificacionRutas;
  private final Clock clock;
  private final ScheduledExecutorService scheduler;
  private final LocalTime hora;

  public PlanificacionScheduler(ServicioPlanificacionRutas servicio) {
    this(servicio, Clock.systemDefaultZone());
  }

  public PlanificacionScheduler(ServicioPlanificacionRutas servicio, Clock clock) {
    this(servicio, clock, Executors.newSingleThreadScheduledExecutor());
  }

  public PlanificacionScheduler(
      ServicioPlanificacionRutas servicio, Clock clock, ScheduledExecutorService scheduler) {
    this(servicio, clock, scheduler, LocalTime.MIDNIGHT);
  }

  public PlanificacionScheduler(
      ServicioPlanificacionRutas servicio,
      Clock clock,
      ScheduledExecutorService scheduler,
      LocalTime hora) {
    this.servicioPlanificacionRutas = servicio;
    this.clock = clock;
    this.scheduler = scheduler;
    this.hora = hora;
  }

  /** El scheduler del despliegue: hora de {@code PLANIFICACION_HORA}, hora de Argentina. */
  public static PlanificacionScheduler desdeEntorno(ServicioPlanificacionRutas servicio) {
    return new PlanificacionScheduler(
        servicio,
        Clock.system(ZoneId.of("America/Argentina/Buenos_Aires")),
        Executors.newSingleThreadScheduledExecutor(),
        hora(System.getenv("PLANIFICACION_HORA")));
  }

  static LocalTime hora(String valor) {
    if (valor == null || valor.isBlank()) {
      return HORA_POR_DEFECTO;
    }
    try {
      return LocalTime.of(Integer.parseInt(valor.trim()), 0);
    } catch (RuntimeException e) {
      LOG.warning("PLANIFICACION_HORA inválida (" + valor + "): se usan las 3.");
      return HORA_POR_DEFECTO;
    }
  }

  /** Cuánto falta para la próxima vez que el reloj marque {@code hora}. */
  public static long milisegundosHastaLaProxima(Clock clock, LocalTime hora) {
    LocalDateTime ahora = LocalDateTime.now(clock);
    LocalDateTime proxima = LocalDate.now(clock).atTime(hora);
    if (!proxima.isAfter(ahora)) {
      proxima = proxima.plusDays(1);
    }
    return Duration.between(ahora, proxima).toMillis();
  }

  public static long milisegundosHastaProximaMedianoche(Clock clock) {
    LocalDateTime ahora = LocalDateTime.now(clock);
    LocalDateTime proximaMedianoche = LocalDate.now(clock).plusDays(1).atTime(LocalTime.MIDNIGHT);
    return Duration.between(ahora, proximaMedianoche).toMillis();
  }

  public void iniciar() {
    long delayInicial = milisegundosHastaLaProxima(clock, hora);
    long periodo = Duration.ofDays(1).toMillis();
    scheduler.scheduleAtFixedRate(
        () -> {
          // Una excepción sin atrapar cancela las ejecuciones siguientes del executor.
          try {
            servicioPlanificacionRutas.planificar();
          } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Falló la planificación diaria de rutas", e);
          }
        },
        delayInicial,
        periodo,
        TimeUnit.MILLISECONDS);
    LOG.info("Planificación de rutas programada todos los días a las " + hora);
  }

  public void detener() {
    scheduler.shutdown();
  }
}
