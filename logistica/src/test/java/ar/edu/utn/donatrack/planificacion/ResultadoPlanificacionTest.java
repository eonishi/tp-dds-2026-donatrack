package ar.edu.utn.donatrack.planificacion;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.donatrack.logistica.Camion;
import ar.edu.utn.donatrack.logistica.Destino;
import ar.edu.utn.donatrack.logistica.planificacion.ResultadoPlanificacion;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResultadoPlanificacionTest {

  private static Destino crearDestino() {
    return new Destino(1L, "Dir 1", List.of());
  }

  @Test
  void crearResultado_ConDatosValidos_DebeTenerAtributosCorrectos() {
    UUID loteId = UUID.randomUUID();
    Camion camion = new Camion("ABC123", 45.0, 2.5, 5000.0);
    Map<Camion, List<Destino>> rutas = Map.of(camion, List.of(crearDestino()));

    ResultadoPlanificacion resultado =
        new ResultadoPlanificacion(loteId, rutas, java.util.Collections.emptyList());

    assertSame(loteId, resultado.loteId());
    assertSame(rutas, resultado.rutas());
  }

  @Test
  void crearResultado_ConLoteNull_DebeLanzarNullPointerException() {
    Camion camion = new Camion("ABC123", 45.0, 2.5, 5000.0);
    Map<Camion, List<Destino>> rutas = Map.of(camion, List.of(crearDestino()));

    assertThrows(
        NullPointerException.class,
        () -> new ResultadoPlanificacion(null, rutas, java.util.Collections.emptyList()));
  }

  @Test
  void crearResultado_ConRutasNull_DebeLanzarNullPointerException() {
    UUID loteId = UUID.randomUUID();

    assertThrows(
        NullPointerException.class,
        () -> new ResultadoPlanificacion(loteId, null, java.util.Collections.emptyList()));
  }

  @Test
  void crearResultado_ConRutasVacias_DebeSerValido() {
    UUID loteId = UUID.randomUUID();
    Map<Camion, List<Destino>> rutas = Map.of();

    ResultadoPlanificacion resultado =
        new ResultadoPlanificacion(loteId, rutas, java.util.Collections.emptyList());

    assertTrue(resultado.rutas().isEmpty());
  }
}
