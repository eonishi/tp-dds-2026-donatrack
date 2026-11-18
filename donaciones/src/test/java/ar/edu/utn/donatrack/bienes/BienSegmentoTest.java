package ar.edu.utn.donatrack.bienes;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BienSegmentoTest {

  private final SubCategoria fideos = new SubCategoria("Fideos", Categoria.ALIMENTO);
  private final LocalDate fecha = LocalDate.of(2026, 7, 10);

  @Test
  void construidoSinEstadoDeUso_DevuelveNoAplica() {
    Bien bien = bien(fideos, fecha, null);

    assertEquals(EstadoUso.NO_APLICA, bien.getEstadoDeUso());
  }

  @Test
  void segmento_SinEstadoDeUsoYConNoAplica_SonElMismoSegmento() {
    Bien sinEstado = bien(fideos, fecha, null);
    Bien noAplica = bien(fideos, fecha, EstadoUso.NO_APLICA);

    assertEquals(noAplica.segmento(), sinEstado.segmento());
    assertTrue(sinEstado.mismoSegmentoQue(noAplica));
  }

  @Test
  void segmento_MismosCampos_EsIgualYConMismoHashCode() {
    Bien a = bien(fideos, fecha, EstadoUso.NUEVO);
    Bien b = bien(fideos, fecha, EstadoUso.NUEVO);

    assertEquals(a.segmento(), b.segmento());
    assertEquals(a.segmento().hashCode(), b.segmento().hashCode());
    assertTrue(a.mismoSegmentoQue(b));
  }

  @Test
  void segmento_CamposDistintos_SonSegmentosDistintos() {
    Bien base = bien(fideos, fecha, EstadoUso.NUEVO);
    SubCategoria arroz = new SubCategoria("Arroz", Categoria.ALIMENTO);

    assertAll(
        () ->
            assertFalse(base.mismoSegmentoQue(bien(arroz, fecha, EstadoUso.NUEVO)), "subcategoria"),
        () ->
            assertFalse(
                base.mismoSegmentoQue(bien(fideos, LocalDate.of(2026, 8, 1), EstadoUso.NUEVO)),
                "fechaVencimiento"),
        () -> assertFalse(base.mismoSegmentoQue(bien(fideos, null, EstadoUso.NUEVO)), "sin fecha"),
        () -> assertFalse(base.mismoSegmentoQue(bien(fideos, fecha, EstadoUso.USADO)), "estado"));
  }

  @Test
  void mismoSegmentoQue_Null_LanzaNullPointerException() {
    Bien bien = bien(fideos, fecha, EstadoUso.NUEVO);

    NullPointerException error =
        assertThrows(NullPointerException.class, () -> bien.mismoSegmentoQue(null));

    assertEquals("otro no puede ser null", error.getMessage());
  }

  private static Bien bien(SubCategoria subcategoria, LocalDate vencimiento, EstadoUso estado) {
    return new Bien("Bien", "", 1.0, UnidadMedida.UNIDADES, vencimiento, estado, subcategoria);
  }
}
