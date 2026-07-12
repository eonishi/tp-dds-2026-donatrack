package ar.edu.utn.donatrack.planificacion;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LoteIdTest {

  @Test
  void crearLoteId_ConUuidValido_DebeTenerElMismoUuid() {
    UUID uuid = UUID.randomUUID();
    LoteId loteId = new LoteId(uuid);
    assertEquals(uuid, loteId.value());
  }

  @Test
  void crearLoteId_ConUuidNull_DebeLanzarNullPointerException() {
    assertThrows(NullPointerException.class, () -> new LoteId(null));
  }

  @Test
  void generar_DevuelveLoteIdDistintoCadaVez() {
    LoteId a = LoteId.generar();
    LoteId b = LoteId.generar();
    assertNotNull(a);
    assertNotNull(b);
    assertNotEquals(a, b);
  }

  @Test
  void mismoUuid_EqualsDevuelveTrue() {
    UUID uuid = UUID.randomUUID();
    LoteId a = new LoteId(uuid);
    LoteId b = new LoteId(uuid);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void distintoUuid_EqualsDevuelveFalse() {
    LoteId a = LoteId.generar();
    LoteId b = LoteId.generar();
    assertNotEquals(a, b);
  }

  @Test
  void loteId_SeDeduplicaEnHashSet() {
    UUID uuid = UUID.randomUUID();
    LoteId a = new LoteId(uuid);
    LoteId b = new LoteId(uuid);
    Set<LoteId> set = new HashSet<>();
    set.add(a);
    set.add(b);
    assertEquals(1, set.size());
  }
}
