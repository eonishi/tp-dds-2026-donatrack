package ar.edu.utn.donatrack.planificacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ar.edu.utn.donatrack.logistica.planificacion.PlanificacionScheduler;
import ar.edu.utn.donatrack.logistica.planificacion.ServicioPlanificacionRutas;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class PlanificacionSchedulerTest {
  @Test
  void iniciar_ConfiguracionSchedulerCorrectamente() {
    ServicioPlanificacionRutas servicio = mock(ServicioPlanificacionRutas.class);
    ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
    Clock clock = Clock.fixed(Instant.parse("2026-09-23T12:00:00Z"), ZoneId.of("UTC"));

    PlanificacionScheduler scheduler = new PlanificacionScheduler(servicio, clock, executor);
    scheduler.iniciar();

    verify(executor)
        .scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
  }

  @Test
  void programaLaPrimeraEjecucionParaLaProximaHoraDeBajaCarga() {
    ZoneId argentina = ZoneId.of("America/Argentina/Buenos_Aires");
    // 12:00 UTC son las 9:00 en Argentina: faltan 18 horas para las 3:00 del día siguiente.
    Clock mediodia = Clock.fixed(Instant.parse("2026-09-23T12:00:00Z"), argentina);
    // 5:59 UTC son las 2:59 en Argentina: falta un minuto.
    Clock casiLasTres = Clock.fixed(Instant.parse("2026-09-23T05:59:00Z"), argentina);

    assertEquals(
        Duration.ofHours(18).toMillis(),
        PlanificacionScheduler.milisegundosHastaLaProxima(mediodia, LocalTime.of(3, 0)));
    assertEquals(
        Duration.ofMinutes(1).toMillis(),
        PlanificacionScheduler.milisegundosHastaLaProxima(casiLasTres, LocalTime.of(3, 0)));
  }

  @Test
  void unaFallaDeLaPlanificacionNoCancelaLasEjecucionesSiguientes() {
    ServicioPlanificacionRutas servicio = mock(ServicioPlanificacionRutas.class);
    doThrow(new IllegalStateException("donaciones no responde")).when(servicio).planificar();
    ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
    Clock clock = Clock.fixed(Instant.parse("2026-09-23T12:00:00Z"), ZoneId.of("UTC"));
    new PlanificacionScheduler(servicio, clock, executor, LocalTime.of(3, 0)).iniciar();

    ArgumentCaptor<Runnable> tarea = ArgumentCaptor.forClass(Runnable.class);
    verify(executor)
        .scheduleAtFixedRate(tarea.capture(), anyLong(), anyLong(), any(TimeUnit.class));
    tarea.getValue().run();

    verify(servicio).planificar();
  }
}
