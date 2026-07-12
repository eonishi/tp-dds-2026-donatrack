package ar.edu.utn.donatrack.planificacion;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.logistica.Camion;
import ar.edu.utn.donatrack.logistica.planificacion.LoteDonaciones;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LoteDonacionesTest {

  private static Camion crearCamionDummy() {
    return new Camion("ABC123", 45.0, 2.5, 5000.0);
  }

  // ── Happy path ─────────────────────────────────────────────────────

  @Test
  void crearLoteDonaciones_ConDatosValidos_DebeTenerAtributosCorrectos() {
    List<Donacion> donaciones = List.of();
    List<Camion> camiones = List.of(crearCamionDummy());

    LoteDonaciones lote = new LoteDonaciones(donaciones, camiones);

    assertSame(donaciones, lote.donaciones());
    assertSame(camiones, lote.camiones());
  }

  // ── Validation ─────────────────────────────────────────────────────

  @Test
  void crearLoteDonaciones_ConDonacionesNull_DebeLanzarNullPointerException() {
    List<Camion> camiones = List.of(crearCamionDummy());
    assertThrows(NullPointerException.class, () -> new LoteDonaciones(null, camiones));
  }

  @Test
  void crearLoteDonaciones_ConCamionesNull_DebeLanzarNullPointerException() {
    List<Donacion> donaciones = List.of();
    assertThrows(NullPointerException.class, () -> new LoteDonaciones(donaciones, null));
  }

  @Test
  void crearLoteDonaciones_ConMasDe100Donaciones_DebeLanzarIllegalArgumentException() {
    List<Donacion> donaciones = new ArrayList<>();
    for (int i = 0; i < 101; i++) {
      donaciones.add(null); // only size matters for this validation
    }
    List<Camion> camiones = List.of(crearCamionDummy());
    assertThrows(IllegalArgumentException.class, () -> new LoteDonaciones(donaciones, camiones));
  }

  @Test
  void crearLoteDonaciones_ConExactamente100Donaciones_DebeSerValido() {
    List<Donacion> donaciones = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      donaciones.add(null);
    }
    List<Camion> camiones = List.of(crearCamionDummy());
    LoteDonaciones lote = new LoteDonaciones(donaciones, camiones);
    assertEquals(100, lote.donaciones().size());
  }

  @Test
  void crearLoteDonaciones_ConCamionesVacios_DebeLanzarIllegalArgumentException() {
    List<Donacion> donaciones = List.of();
    List<Camion> camiones = List.of();
    assertThrows(IllegalArgumentException.class, () -> new LoteDonaciones(donaciones, camiones));
  }
}
