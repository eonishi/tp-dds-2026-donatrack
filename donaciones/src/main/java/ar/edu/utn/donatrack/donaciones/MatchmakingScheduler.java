package ar.edu.utn.donatrack.donaciones;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MatchmakingScheduler {
  private final ServicioMatchmakingDonaciones servicioMatchmakingDonaciones;
  private final Clock clock;
  private final ScheduledExecutorService scheduler;

  public MatchmakingScheduler(ServicioMatchmakingDonaciones servicioMatchmakingDonaciones) {
    this(servicioMatchmakingDonaciones, Clock.systemDefaultZone());
  }

  public MatchmakingScheduler(
      ServicioMatchmakingDonaciones servicioMatchmakingDonaciones, Clock clock) {
    this(servicioMatchmakingDonaciones, clock, Executors.newSingleThreadScheduledExecutor());
  }

  public MatchmakingScheduler(
      ServicioMatchmakingDonaciones servicioMatchmakingDonaciones,
      Clock clock,
      ScheduledExecutorService scheduler) {
    this.servicioMatchmakingDonaciones = servicioMatchmakingDonaciones;
    this.clock = clock;
    this.scheduler = scheduler;
  }

  public void iniciar() {
    long delayInicial = milisegundosHastaProximaMedianoche(clock);
    long periodo = Duration.ofDays(1).toMillis();
    scheduler.scheduleAtFixedRate(this::ejecutar, delayInicial, periodo, TimeUnit.MILLISECONDS);
  }

  public void detener() {
    scheduler.shutdown();
  }

  void ejecutar() {
    servicioMatchmakingDonaciones.ejecutarMatchmakingDiario(LocalDate.now(clock));
  }

  public static long milisegundosHastaProximaMedianoche(Clock clock) {
    LocalDateTime ahora = LocalDateTime.now(clock);
    LocalDateTime proximaMedianoche = LocalDate.now(clock).plusDays(1).atTime(LocalTime.MIDNIGHT);
    return Duration.between(ahora, proximaMedianoche).toMillis();
  }
}
