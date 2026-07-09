package ar.edu.utn.donatrack.donaciones;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.donatrack.bienes.Categoria;
import ar.edu.utn.donatrack.bienes.EstadoUso;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SegmentacionKeyTest {

  private final SubCategoria categoria = new SubCategoria("Fideos", Categoria.ALIMENTO);
  private final LocalDate fecha = LocalDate.of(2026, 7, 10);
  private final EstadoUso estado = EstadoUso.NUEVO;

  @Test
  void equals_Simetrico_CuandoFechasDentroDe5Dias() {
    SegmentacionKey keyA = new SegmentacionKey(categoria, LocalDate.of(2026, 7, 10), estado);
    SegmentacionKey keyB = new SegmentacionKey(categoria, LocalDate.of(2026, 7, 12), estado);

    assertEquals(keyA.equals(keyB), keyB.equals(keyA),
        "equals must be symmetric: A.equals(B) should equal B.equals(A)");
  }

  @Test
  void equals_MismaReferencia_True() {
    SegmentacionKey key = new SegmentacionKey(categoria, fecha, estado);
    assertTrue(key.equals(key));
  }

  @Test
  void equals_CamposIguales_True() {
    SegmentacionKey keyA = new SegmentacionKey(categoria, fecha, estado);
    SegmentacionKey keyB = new SegmentacionKey(categoria, fecha, estado);
    assertEquals(keyA, keyB);
  }

  @Test
  void equals_Null_False() {
    SegmentacionKey key = new SegmentacionKey(categoria, fecha, estado);
    assertFalse(key.equals(null));
  }

  @Test
  void equals_TipoDiferente_False() {
    SegmentacionKey key = new SegmentacionKey(categoria, fecha, estado);
    assertFalse(key.equals("not a SegmentacionKey"));
  }

  @Test
  void equals_CamposDistintos_False() {
    SubCategoria otraCategoria = new SubCategoria("Arroz", Categoria.ALIMENTO);
    SegmentacionKey base = new SegmentacionKey(categoria, fecha, estado);
    assertAll(
        () -> assertFalse(base.equals(new SegmentacionKey(otraCategoria, fecha, estado)),
            "different subcategoria"),
        () -> assertFalse(base.equals(new SegmentacionKey(categoria, LocalDate.of(2026, 8, 1), estado)),
            "different fechaVencimiento"),
        () -> assertFalse(base.equals(new SegmentacionKey(categoria, null, estado)),
            "null fechaVencimiento"),
        () -> assertFalse(base.equals(new SegmentacionKey(categoria, fecha, EstadoUso.USADO)),
            "different estadoUso"));
  }

  @Test
  void hashCode_Consistente() {
    SegmentacionKey keyA = new SegmentacionKey(categoria, fecha, estado);
    SegmentacionKey keyB = new SegmentacionKey(categoria, fecha, estado);
    assertEquals(keyA.hashCode(), keyB.hashCode());
  }
}
