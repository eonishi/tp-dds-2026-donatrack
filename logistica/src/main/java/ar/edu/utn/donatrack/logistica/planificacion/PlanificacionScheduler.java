package ar.edu.utn.donatrack.logistica.planificacion;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PlanificacionScheduler {
  private final ServicioPlanificacionRutas servicioPlanificacionRutas;
  private final Clock clock;
  private final ScheduledExecutorService scheduler;

  public PlanificacionScheduler(ServicioPlanificacionRutas servicio) {
    this(servicio, Clock.systemDefaultZone());
  }

  public PlanificacionScheduler(ServicioPlanificacionRutas servicio, Clock clock) {
    this(servicio, clock, Executors.newSingleThreadScheduledExecutor());
  }

  public PlanificacionScheduler(
      ServicioPlanificacionRutas servicio, Clock clock, ScheduledExecutorService scheduler) {
    this.servicioPlanificacionRutas = servicio;
    this.clock = clock;
    this.scheduler = scheduler;
  }

  public static long milisegundosHastaProximaMedianoche(Clock clock) {
    LocalDateTime ahora = LocalDateTime.now(clock);
    LocalDateTime proximaMedianoche = LocalDate.now(clock).plusDays(1).atTime(LocalTime.MIDNIGHT);
    return Duration.between(ahora, proximaMedianoche).toMillis();
  }

  public void iniciar() {
    long delayInicial = milisegundosHastaProximaMedianoche(clock);
    long periodo = Duration.ofDays(1).toMillis();
    scheduler.scheduleAtFixedRate(
        servicioPlanificacionRutas::planificar, delayInicial, periodo, TimeUnit.MILLISECONDS);
  }

  public void detener() {
    scheduler.shutdown();
  }
}
