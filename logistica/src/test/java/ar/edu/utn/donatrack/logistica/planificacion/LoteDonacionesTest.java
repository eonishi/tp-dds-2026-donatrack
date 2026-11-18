package ar.edu.utn.donatrack.logistica.planificacion;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.donatrack.logistica.Camion;
import ar.edu.utn.donatrack.logistica.monitoreo.Coordenada;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LoteDonacionesTest {

  private static final String URL_CALLBACK = "http://logistica.test/callback";

  private static Camion crearCamionDummy() {
    return new Camion("ABC123", 45.0, 2.5, 5000.0);
  }

  private static DonacionPlanificable crearPlanificable() {
    return new DonacionPlanificable(
        UUID.randomUUID(), 1L, "Dir 1", new Coordenada(-34.6, -58.4), 1.0, 1.0);
  }

  // ── Cycle 1: Creación y validación ─────────────────────────────────

  @Test
  void crearLoteDonaciones_ConDatosValidos_DebeTenerAtributosCorrectos() {
    List<DonacionPlanificable> donaciones = List.of();
    List<Camion> camiones = List.of(crearCamionDummy());

    LoteDonaciones lote = new LoteDonaciones(donaciones, camiones, URL_CALLBACK);

    assertSame(donaciones, lote.donaciones());
    assertSame(camiones, lote.camiones());
  }

  @Test
  void crearLoteDonaciones_ConDonacionesNull_DebeLanzarNullPointerException() {
    List<Camion> camiones = List.of(crearCamionDummy());

    assertThrows(
        NullPointerException.class, () -> new LoteDonaciones(null, camiones, URL_CALLBACK));
  }

  @Test
  void crearLoteDonaciones_ConMasDe100Donaciones_DebeLanzarIllegalArgumentException() {
    List<DonacionPlanificable> donaciones = new ArrayList<>();
    for (int i = 0; i < 101; i++) {
      donaciones.add(crearPlanificable());
    }
    List<Camion> camiones = List.of(crearCamionDummy());

    assertThrows(
        IllegalArgumentException.class,
        () -> new LoteDonaciones(donaciones, camiones, URL_CALLBACK));
  }

  @Test
  void crearLoteDonaciones_ConExactamente100Donaciones_DebeSerValido() {
    List<DonacionPlanificable> donaciones = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      donaciones.add(crearPlanificable());
    }
    List<Camion> camiones = List.of(crearCamionDummy());

    LoteDonaciones lote = new LoteDonaciones(donaciones, camiones, URL_CALLBACK);

    assertEquals(100, lote.donaciones().size());
  }

  @Test
  void crearLoteDonaciones_ConCamionesNull_DebeLanzarNullPointerException() {
    List<DonacionPlanificable> donaciones = List.of(crearPlanificable());

    assertThrows(
        NullPointerException.class, () -> new LoteDonaciones(donaciones, null, URL_CALLBACK));
  }

  @Test
  void crearLoteDonaciones_ConCamionesVacios_DebeLanzarIllegalArgumentException() {
    List<DonacionPlanificable> donaciones = List.of(crearPlanificable());

    assertThrows(
        IllegalArgumentException.class,
        () -> new LoteDonaciones(donaciones, List.of(), URL_CALLBACK));
  }

  @Test
  void crearLoteDonaciones_ConUrlCallbackVacia_DebeLanzarIllegalArgumentException() {
    List<DonacionPlanificable> donaciones = List.of(crearPlanificable());
    List<Camion> camiones = List.of(crearCamionDummy());

    assertThrows(
        IllegalArgumentException.class, () -> new LoteDonaciones(donaciones, camiones, " "));
  }
}
