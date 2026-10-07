package ar.edu.utn.donatrack.logistica;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DestinoTest {

  // ── Helpers ────────────────────────────────────────────────────────

  private static DonacionId crearDonacionId() {
    return new DonacionId(UUID.randomUUID());
  }

  // ── Cycle 1: Creación ──────────────────────────────────────────────

  @Test
  void crearDestino_ConEntidadIdNull_DebeLanzarNullPointerException() {
    assertThrows(NullPointerException.class, () -> new Destino(null, "Calle 123", List.of()));
  }

  @Test
  void crearDestino_ConDireccionNull_DebeLanzarNullPointerException() {
    assertThrows(
        NullPointerException.class,
        () -> new Destino(new EntidadBeneficiariaId(1L), null, List.of()));
  }

  @Test
  void crearDestino_ConDonacionesNull_DebeLanzarNullPointerException() {
    assertThrows(
        NullPointerException.class,
        () -> new Destino(new EntidadBeneficiariaId(1L), "Calle 123", null));
  }

  @Test
  void crearDestino_ConDonacionNull_DebeLanzarNullPointerException() {
    List<DonacionId> donaciones = Collections.singletonList(null);

    assertThrows(
        NullPointerException.class,
        () -> new Destino(new EntidadBeneficiariaId(1L), "Calle 123", donaciones));
  }

  @Test
  void crearDestino_ConDatosValidos_DebeTenerAtributosCorrectos() {
    EntidadBeneficiariaId entidadId = new EntidadBeneficiariaId(1L);
    DonacionId donacionId = crearDonacionId();

    Destino destino = new Destino(entidadId, "Calle 123", List.of(donacionId));

    assertSame(entidadId, destino.entidadId());
    assertEquals("Calle 123", destino.direccion());
    assertEquals(List.of(donacionId), destino.donaciones());
  }

  @Test
  void crearDestino_SinDonaciones_DebeTenerListaVacia() {
    Destino destino = new Destino(new EntidadBeneficiariaId(1L), "Calle 123", List.of());

    assertTrue(destino.donaciones().isEmpty());
  }

  @Test
  void crearDestino_MantieneElOrdenDeLasDonaciones() {
    DonacionId d1 = crearDonacionId();
    DonacionId d2 = crearDonacionId();
    DonacionId d3 = crearDonacionId();

    Destino destino = new Destino(new EntidadBeneficiariaId(1L), "Calle 123", List.of(d1, d2, d3));

    assertEquals(List.of(d1, d2, d3), destino.donaciones());
  }

  // ── Cycle 2: Query ─────────────────────────────────────────────────

  @Test
  void getDonaciones_AlModificarLista_DebeLanzarUnsupportedOperationException() {
    Destino destino =
        new Destino(new EntidadBeneficiariaId(1L), "Calle 123", List.of(crearDonacionId()));

    assertThrows(
        UnsupportedOperationException.class, () -> destino.donaciones().add(crearDonacionId()));
  }

  @Test
  void getDonaciones_AlModificarLaListaOriginal_NoAfectaAlDestino() {
    List<DonacionId> original = new ArrayList<>();
    original.add(crearDonacionId());
    Destino destino = new Destino(new EntidadBeneficiariaId(1L), "Calle 123", original);

    original.add(crearDonacionId());

    assertEquals(1, destino.donaciones().size());
  }

  // ── Phase 1: equals/hashCode ───────────────────────────────────────

  @Test
  void mismosDatos_EqualsDevuelveTrue() {
    EntidadBeneficiariaId entidadId = new EntidadBeneficiariaId(1L);
    DonacionId donacionId = crearDonacionId();
    Destino a = new Destino(entidadId, "Calle 123", List.of(donacionId));
    Destino b = new Destino(entidadId, "Calle 123", List.of(donacionId));

    assertEquals(a, b);
  }

  @Test
  void mismosDatos_HashCodeEsIgual() {
    EntidadBeneficiariaId entidadId = new EntidadBeneficiariaId(1L);
    DonacionId donacionId = crearDonacionId();
    Destino a = new Destino(entidadId, "Calle 123", List.of(donacionId));
    Destino b = new Destino(entidadId, "Calle 123", List.of(donacionId));

    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void distintaEntidadId_EqualsDevuelveFalse() {
    Destino a = new Destino(new EntidadBeneficiariaId(1L), "Calle 123", List.of());
    Destino b = new Destino(new EntidadBeneficiariaId(2L), "Calle 123", List.of());

    assertNotEquals(a, b);
  }

  @Test
  void mismaEntidadIdDistintaDireccion_EqualsDevuelveFalse() {
    EntidadBeneficiariaId entidadId = new EntidadBeneficiariaId(1L);
    Destino a = new Destino(entidadId, "Calle 123", List.of());
    Destino b = new Destino(entidadId, "Calle 456", List.of());

    assertNotEquals(a, b);
  }

  @Test
  void mismaEntidadIdDistintasDonaciones_EqualsDevuelveFalse() {
    EntidadBeneficiariaId entidadId = new EntidadBeneficiariaId(1L);
    Destino a = new Destino(entidadId, "Calle 123", List.of(crearDonacionId()));
    Destino b = new Destino(entidadId, "Calle 123", List.of(crearDonacionId(), crearDonacionId()));

    assertNotEquals(a, b);
  }
}
