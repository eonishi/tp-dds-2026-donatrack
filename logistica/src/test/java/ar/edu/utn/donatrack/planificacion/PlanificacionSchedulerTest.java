package ar.edu.utn.donatrack.planificacion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ar.edu.utn.donatrack.logistica.planificacion.PlanificacionScheduler;
import ar.edu.utn.donatrack.logistica.planificacion.ServicioPlanificacionRutas;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class PlanificacionSchedulerTest {
    @Test 
    void iniciar_ConfiguracionSchedulerCorrectamente() {
        ServicioPlanificacionRutas servicio = mock(ServicioPlanificacionRutas.class);
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-23T12:00:00Z"), ZoneId.of("UTC"));

        PlanificacionScheduler scheduler = new PlanificacionScheduler(servicio, clock, executor);
        scheduler.iniciar();

        verify(executor).scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
    }
}
