package ar.edu.utn.donatrack.beneficiarios;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.bienes.Categoria;
import ar.edu.utn.donatrack.bienes.EstadoUso;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.bienes.UnidadMedida;
import org.junit.jupiter.api.Test;

class NecesidadTest {

  @Test
  void constructorConCamposPropios_AsignaSubcategoria() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Necesidad necesidad =
        new Necesidad(fideos, 5.0, UnidadMedida.KILOGRAMOS, "Fideos para el comedor");

    assertEquals(fideos, necesidad.getSubcategoria());
  }

  @Test
  void constructorConCamposPropios_AsignaCantidadObjetivo() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Necesidad necesidad =
        new Necesidad(fideos, 5.0, UnidadMedida.KILOGRAMOS, "Fideos para el comedor");

    assertEquals(5.0, necesidad.getCantidadObjetivo(), 0.001);
  }

  @Test
  void constructorConCamposPropios_AsignaUnidad() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Necesidad necesidad =
        new Necesidad(fideos, 5.0, UnidadMedida.KILOGRAMOS, "Fideos para el comedor");

    assertEquals(UnidadMedida.KILOGRAMOS, necesidad.getUnidad());
  }

  @Test
  void constructorConCamposPropios_AsignaDescripcion() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Necesidad necesidad =
        new Necesidad(fideos, 5.0, UnidadMedida.KILOGRAMOS, "Fideos para el comedor");

    assertEquals("Fideos para el comedor", necesidad.getDescripcion());
  }

  @Test
  void necesidad_RecienCreada_NoEstaSatisfecha() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Necesidad necesidad = new Necesidad(fideos, 10.0, UnidadMedida.KILOGRAMOS, "Fideos");

    assertFalse(necesidad.estaSatisfecha());
  }

  @Test
  void necesidad_AlcanzaObjetivo_EstaSatisfecha() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Necesidad necesidad = new Necesidad(fideos, 10.0, UnidadMedida.KILOGRAMOS, "Fideos");

    necesidad.recibirDonacion(10.0);

    assertTrue(necesidad.estaSatisfecha());
  }

  @Test
  void necesidad_ConDonacionesParciales_NoEstaSatisfecha() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Necesidad necesidad = new Necesidad(fideos, 10.0, UnidadMedida.KILOGRAMOS, "Fideos");

    necesidad.recibirDonacion(7.0);

    assertFalse(necesidad.estaSatisfecha());
  }

  @Test
  void esCompatibleCon_BienDeLaMismaSubcategoria_DevuelveTrue() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Necesidad necesidad = new Necesidad(fideos, 10.0, UnidadMedida.KILOGRAMOS, "Fideos");

    assertTrue(necesidad.esCompatibleCon(bienDe(fideos, UnidadMedida.KILOGRAMOS)));
  }

  @Test
  void esCompatibleCon_BienDeOtraSubcategoria_DevuelveFalse() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    SubCategoria sillas = new SubCategoria("Sillas", Categoria.MOBILIARIO);
    Necesidad necesidad = new Necesidad(fideos, 10.0, UnidadMedida.KILOGRAMOS, "Fideos");

    assertFalse(necesidad.esCompatibleCon(bienDe(sillas, UnidadMedida.UNIDADES)));
  }

  @Test
  void esCompatibleCon_BienNull_LanzaNullPointerException() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Necesidad necesidad = new Necesidad(fideos, 10.0, UnidadMedida.KILOGRAMOS, "Fideos");

    assertThrows(NullPointerException.class, () -> necesidad.esCompatibleCon(null));
  }

  @Test
  void tieneMismaUnidadQue_BienDeLaMismaUnidad_DevuelveTrue() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Necesidad necesidad = new Necesidad(fideos, 10.0, UnidadMedida.KILOGRAMOS, "Fideos");

    assertTrue(necesidad.tieneMismaUnidadQue(bienDe(fideos, UnidadMedida.KILOGRAMOS)));
  }

  @Test
  void tieneMismaUnidadQue_BienDeOtraUnidad_DevuelveFalse() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Necesidad necesidad = new Necesidad(fideos, 10.0, UnidadMedida.KILOGRAMOS, "Fideos");

    assertFalse(necesidad.tieneMismaUnidadQue(bienDe(fideos, UnidadMedida.UNIDADES)));
  }

  @Test
  void tieneMismaUnidadQue_BienNull_LanzaNullPointerException() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Necesidad necesidad = new Necesidad(fideos, 10.0, UnidadMedida.KILOGRAMOS, "Fideos");

    assertThrows(NullPointerException.class, () -> necesidad.tieneMismaUnidadQue(null));
  }

  @Test
  void alcanzaObjetivoCon_CantidadIgualAlObjetivo_DevuelveTrue() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Necesidad necesidad = new Necesidad(fideos, 10.0, UnidadMedida.KILOGRAMOS, "Fideos");

    assertTrue(necesidad.alcanzaObjetivoCon(10.0), "El objetivo exacto debe alcanzar");
  }

  @Test
  void alcanzaObjetivoCon_CantidadMayorAlObjetivo_DevuelveTrue() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Necesidad necesidad = new Necesidad(fideos, 10.0, UnidadMedida.KILOGRAMOS, "Fideos");

    assertTrue(necesidad.alcanzaObjetivoCon(10.5));
  }

  @Test
  void alcanzaObjetivoCon_CantidadMenorAlObjetivo_DevuelveFalse() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Necesidad necesidad = new Necesidad(fideos, 10.0, UnidadMedida.KILOGRAMOS, "Fideos");

    assertFalse(necesidad.alcanzaObjetivoCon(9.5));
  }

  private static Bien bienDe(SubCategoria subcategoria, UnidadMedida unidad) {
    return new Bien("Bien", "", 1.0, unidad, null, EstadoUso.NO_APLICA, subcategoria);
  }
}
