package ar.edu.utn.donatrack.logistica;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DestinoTest {

  // ── Helpers ────────────────────────────────────────────────────────

  private static UUID crearDonacionUuid() {
    return UUID.randomUUID();
  }

  // ── Cycle 1: Creación ──────────────────────────────────────────────

  @Test
  void crearDestino_ConEntidadIdNull_DebeLanzarNullPointerException() {
    assertThrows(NullPointerException.class, () -> new Destino(null, "Calle 123", List.of()));
  }

  @Test
  void crearDestino_ConDireccionNull_DebeLanzarNullPointerException() {
    assertThrows(NullPointerException.class, () -> new Destino(1L, null, List.of()));
  }

  @Test
  void crearDestino_ConDonacionesNull_DebeLanzarNullPointerException() {
    assertThrows(NullPointerException.class, () -> new Destino(1L, "Calle 123", null));
  }

  @Test
  void crearDestino_ConDonacionNull_DebeLanzarNullPointerException() {
    List<UUID> donaciones = Collections.singletonList(null);

    assertThrows(NullPointerException.class, () -> new Destino(1L, "Calle 123", donaciones));
  }

  @Test
  void crearDestino_ConDatosValidos_DebeTenerAtributosCorrectos() {
    Long entidadId = 1L;
    UUID donacionId = crearDonacionUuid();

    Destino destino = new Destino(entidadId, "Calle 123", List.of(donacionId));

    assertSame(entidadId, destino.entidadId());
    assertEquals("Calle 123", destino.direccion());
    assertEquals(List.of(donacionId), destino.donaciones());
  }

  @Test
  void crearDestino_SinDonaciones_DebeTenerListaVacia() {
    Destino destino = new Destino(1L, "Calle 123", List.of());

    assertTrue(destino.donaciones().isEmpty());
  }

  @Test
  void crearDestino_MantieneElOrdenDeLasDonaciones() {
    UUID d1 = crearDonacionUuid();
    UUID d2 = crearDonacionUuid();
    UUID d3 = crearDonacionUuid();

    Destino destino = new Destino(1L, "Calle 123", List.of(d1, d2, d3));

    assertEquals(List.of(d1, d2, d3), destino.donaciones());
  }

  // ── Cycle 2: Query ─────────────────────────────────────────────────

  @Test
  void getDonaciones_AlModificarLista_DebeLanzarUnsupportedOperationException() {
    Destino destino = new Destino(1L, "Calle 123", List.of(crearDonacionUuid()));

    assertThrows(
        UnsupportedOperationException.class, () -> destino.donaciones().add(crearDonacionUuid()));
  }

  @Test
  void getDonaciones_AlModificarLaListaOriginal_NoAfectaAlDestino() {
    List<UUID> original = new ArrayList<>();
    original.add(crearDonacionUuid());
    Destino destino = new Destino(1L, "Calle 123", original);

    original.add(crearDonacionUuid());

    assertEquals(1, destino.donaciones().size());
  }

  // ── Phase 1: equals/hashCode ───────────────────────────────────────

  @Test
  void mismosDatos_EqualsDevuelveTrue() {
    Long entidadId = 1L;
    UUID donacionId = crearDonacionUuid();
    Destino a = new Destino(entidadId, "Calle 123", List.of(donacionId));
    Destino b = new Destino(entidadId, "Calle 123", List.of(donacionId));

    assertEquals(a, b);
  }

  @Test
  void mismosDatos_HashCodeEsIgual() {
    Long entidadId = 1L;
    UUID donacionId = crearDonacionUuid();
    Destino a = new Destino(entidadId, "Calle 123", List.of(donacionId));
    Destino b = new Destino(entidadId, "Calle 123", List.of(donacionId));

    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void distintaEntidadId_EqualsDevuelveFalse() {
    Destino a = new Destino(1L, "Calle 123", List.of());
    Destino b = new Destino(2L, "Calle 123", List.of());

    assertNotEquals(a, b);
  }

  @Test
  void mismaEntidadIdDistintaDireccion_EqualsDevuelveFalse() {
    Long entidadId = 1L;
    Destino a = new Destino(entidadId, "Calle 123", List.of());
    Destino b = new Destino(entidadId, "Calle 456", List.of());

    assertNotEquals(a, b);
  }

  @Test
  void mismaEntidadIdDistintasDonaciones_EqualsDevuelveFalse() {
    Long entidadId = 1L;
    Destino a = new Destino(entidadId, "Calle 123", List.of(crearDonacionUuid()));
    Destino b =
        new Destino(entidadId, "Calle 123", List.of(crearDonacionUuid(), crearDonacionUuid()));

    assertNotEquals(a, b);
  }
}
