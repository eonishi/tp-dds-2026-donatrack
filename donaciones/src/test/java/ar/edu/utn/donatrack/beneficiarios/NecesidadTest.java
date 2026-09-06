package ar.edu.utn.donatrack.beneficiarios;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.donatrack.bienes.Categoria;
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
  void necesidad_SinRecepciones_NoEstaSatisfecha() {
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
}
