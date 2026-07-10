package ar.edu.utn.donatrack.donaciones;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.beneficiarios.Necesidad;
import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.bienes.Categoria;
import ar.edu.utn.donatrack.bienes.EstadoUso;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.bienes.UnidadMedida;
import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.Genero;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.TipoDocumento;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlgoritmoCompatibilidadSemanticaTest {

  private static Donante crearDonanteDummy() {
    return new PersonaHumana(
        new CorreoElectronico("test@test.com"),
        "Juan",
        "Perez",
        30,
        "12345678",
        TipoDocumento.DNI,
        Genero.MASCULINO,
        "Calle Falsa 123");
  }

  @Test
  void puntajeMaximo_ConTodosLosCriterios_NoSupera75() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    EntidadBeneficiaria entidad = new EntidadBeneficiaria("Comedor", "Dir", "Tel");
    entidad.agregarNecesidadExtraordinaria(
        new Necesidad(fideos, 10.0, UnidadMedida.KILOGRAMOS, "Fideos"));

    // Una donación que cumple subcategoria, unidad y cantidad suficiente
    Bien bienFideos =
        new Bien("Fideos", "", 15.0, UnidadMedida.KILOGRAMOS, null, EstadoUso.NO_APLICA, fideos);
    Donacion donacion = new Donacion(crearDonanteDummy(), List.of(bienFideos));

    var algoritmo = new AlgoritmoCompatibilidadSemantica();

    // En lugar de llamar puntajeNecesidad directamente (privado),
    // rankeamos y verificamos que el puntaje max sea ≤ 75
    AlgoritmoMatchmaking mock = algoritmo;
    RankingMatchmaking ranking = mock.rankear(donacion, List.of(entidad), LocalDate.now());

    if (!ranking.getCandidatos().isEmpty()) {
      assertTrue(ranking.getCandidatos().get(0).getPuntaje() <= 75);
    }
  }

  @Test
  void puntaje_SubcategoriaDiferente_RetornaCero() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    SubCategoria sillas = new SubCategoria("Sillas", Categoria.MOBILIARIO);
    EntidadBeneficiaria entidad = new EntidadBeneficiaria("Comedor", "Dir", "Tel");
    entidad.agregarNecesidadExtraordinaria(
        new Necesidad(fideos, 10.0, UnidadMedida.KILOGRAMOS, "Fideos"));

    Bien bienSillas =
        new Bien("Silla", "", 1.0, UnidadMedida.UNIDADES, null, EstadoUso.NUEVO, sillas);
    Donacion donacion = new Donacion(crearDonanteDummy(), List.of(bienSillas));

    var ranking =
        new AlgoritmoCompatibilidadSemantica()
            .rankear(donacion, List.of(entidad), LocalDate.of(2026, 7, 9));

    assertTrue(ranking.getCandidatos().isEmpty());
  }

  @Test
  void puntaje_SubcategoriaUnidadYCantidad_Suma75() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    EntidadBeneficiaria entidad = new EntidadBeneficiaria("Comedor", "Dir", "Tel");
    entidad.agregarNecesidadExtraordinaria(
        new Necesidad(fideos, 10.0, UnidadMedida.KILOGRAMOS, "Fideos"));

    Bien bienFideos =
        new Bien("Fideos", "", 15.0, UnidadMedida.KILOGRAMOS, null, EstadoUso.NO_APLICA, fideos);
    Donacion donacion = new Donacion(crearDonanteDummy(), List.of(bienFideos));

    var algoritmo = new AlgoritmoCompatibilidadSemantica();

    // Accedemos via helper: test directo del puntaje interno
    // creamos solo una entidad con una necesidad y verificamos ranking
    RankingMatchmaking ranking =
        algoritmo.rankear(donacion, List.of(entidad), LocalDate.of(2026, 7, 9));

    assertEquals(1, ranking.getCandidatos().size());
    assertEquals(75, ranking.getCandidatos().get(0).getPuntaje());
  }
}
