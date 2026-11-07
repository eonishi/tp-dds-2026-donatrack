package ar.edu.utn.donatrack.donaciones;

import static ar.edu.utn.donatrack.testsupport.FabricaDePrueba.nuevaDonacion;
import static ar.edu.utn.donatrack.testsupport.FabricaDePrueba.nuevaEntidad;
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
import ar.edu.utn.donatrack.propuestas.matchmaking.AlgoritmoCompatibilidadSemantica;
import ar.edu.utn.donatrack.propuestas.matchmaking.MatchCandidato;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlgoritmoCompatibilidadSemanticaTest {

  private static Donante crearDonanteDummy() {
    return new PersonaHumana(
        new CorreoElectronico("test@test.com"),
        "Juan",
        "Perez",
        "12345678",
        TipoDocumento.DNI,
        Genero.MASCULINO,
        "Calle Falsa 123");
  }

  @Test
  void puntaje_ConTodosLosCriterios_CoincideConElPuntajeDeLaEntidad() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    EntidadBeneficiaria entidad = nuevaEntidad("Comedor");
    entidad.agregarNecesidadExtraordinaria(
        new Necesidad(fideos, 10.0, UnidadMedida.KILOGRAMOS, "Fideos"));

    // Una donación que cumple subcategoria, unidad y cantidad suficiente
    Bien bienFideos =
        new Bien("Fideos", "", 15.0, UnidadMedida.KILOGRAMOS, null, EstadoUso.NO_APLICA, fideos);
    Donacion donacion = nuevaDonacion(crearDonanteDummy(), List.of(bienFideos));

    var algoritmo = new AlgoritmoCompatibilidadSemantica();

    List<MatchCandidato> candidatos =
        algoritmo.rankear(donacion, List.of(entidad), LocalDate.of(2026, 7, 9), List.of());

    assertEquals(
        75,
        entidad.calcularPuntajeCompatibilidad(donacion),
        "La entidad responde la compatibilidad con la donación");
    assertEquals(1, candidatos.size());
    assertEquals(
        entidad.calcularPuntajeCompatibilidad(donacion),
        candidatos.get(0).getPuntaje(),
        "El algoritmo publica el puntaje que calcula la entidad");
  }

  @Test
  void rankear_ConNecesidadesDeDistintaUnidadYObjetivo_UsaElPuntajeMaximo() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    EntidadBeneficiaria objetivoLejano = nuevaEntidad("Objetivo lejano");
    objetivoLejano.agregarNecesidadExtraordinaria(
        new Necesidad(fideos, 100.0, UnidadMedida.KILOGRAMOS, "Muchos fideos"));
    EntidadBeneficiaria objetivoCercano = nuevaEntidad("Objetivo cercano");
    objetivoCercano.agregarNecesidadExtraordinaria(
        new Necesidad(fideos, 5.0, UnidadMedida.KILOGRAMOS, "Pocos fideos"));
    Donacion donacion =
        nuevaDonacion(
            crearDonanteDummy(),
            List.of(
                new Bien(
                    "Fideos",
                    "",
                    15.0,
                    UnidadMedida.KILOGRAMOS,
                    null,
                    EstadoUso.NO_APLICA,
                    fideos)));

    var candidatos =
        new AlgoritmoCompatibilidadSemantica()
            .rankear(
                donacion,
                List.of(objetivoLejano, objetivoCercano),
                LocalDate.of(2026, 7, 9),
                List.of());

    assertEquals(2, candidatos.size());
    assertEquals(
        "Objetivo cercano",
        candidatos.get(0).getEntidadBeneficiaria().getRazonSocial(),
        "Quien alcanza su objetivo con la donación puntúa más alto");
    assertEquals(65, candidatos.get(1).getPuntaje(), "Subcategoría y unidad suman 65");
  }

  @Test
  void rankear_ConMasDe10Entidades_RetornaMaximo10Candidatos() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Donacion donacion =
        nuevaDonacion(
            crearDonanteDummy(),
            List.of(
                new Bien(
                    "Fideos",
                    "",
                    15.0,
                    UnidadMedida.KILOGRAMOS,
                    null,
                    EstadoUso.NO_APLICA,
                    fideos)));

    // Create 15 entidades with matching subcategoria
    var entidades =
        java.util.stream.IntStream.range(0, 15)
            .mapToObj(
                i -> {
                  EntidadBeneficiaria e = nuevaEntidad("Entidad " + i);
                  e.agregarNecesidadExtraordinaria(
                      new Necesidad(fideos, 5.0, UnidadMedida.KILOGRAMOS, "Fideos"));
                  return e;
                })
            .toList();

    var candidatos =
        new AlgoritmoCompatibilidadSemantica()
            .rankear(donacion, entidades, LocalDate.of(2026, 7, 9), List.of());

    assertTrue(
        candidatos.size() <= 10, "Compatibilidad semantica debe retornar máximo 10 candidatos");
    assertEquals(
        10, candidatos.size(), "Con 15 entidades compatibles debe retornar exactamente 10");
    // Verify sort order: highest puntaje first
    for (int i = 1; i < candidatos.size(); i++) {
      assertTrue(
          candidatos.get(i - 1).getPuntaje() >= candidatos.get(i).getPuntaje(),
          "Candidatos deben estar ordenados por puntaje descendente");
    }
  }

  @Test
  void puntaje_SubcategoriaDiferente_RetornaCero() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    SubCategoria sillas = new SubCategoria("Sillas", Categoria.MOBILIARIO);
    EntidadBeneficiaria entidad = nuevaEntidad("Comedor");
    entidad.agregarNecesidadExtraordinaria(
        new Necesidad(fideos, 10.0, UnidadMedida.KILOGRAMOS, "Fideos"));

    Bien bienSillas =
        new Bien("Silla", "", 1.0, UnidadMedida.UNIDADES, null, EstadoUso.NUEVO, sillas);
    Donacion donacion = nuevaDonacion(crearDonanteDummy(), List.of(bienSillas));

    var candidatos =
        new AlgoritmoCompatibilidadSemantica()
            .rankear(donacion, List.of(entidad), LocalDate.of(2026, 7, 9), List.of());

    assertTrue(candidatos.isEmpty());
  }

  @Test
  void puntaje_SubcategoriaUnidadYCantidad_Suma75() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    EntidadBeneficiaria entidad = nuevaEntidad("Comedor");
    entidad.agregarNecesidadExtraordinaria(
        new Necesidad(fideos, 10.0, UnidadMedida.KILOGRAMOS, "Fideos"));

    Bien bienFideos =
        new Bien("Fideos", "", 15.0, UnidadMedida.KILOGRAMOS, null, EstadoUso.NO_APLICA, fideos);
    Donacion donacion = nuevaDonacion(crearDonanteDummy(), List.of(bienFideos));

    var algoritmo = new AlgoritmoCompatibilidadSemantica();

    List<MatchCandidato> candidatos =
        algoritmo.rankear(donacion, List.of(entidad), LocalDate.of(2026, 7, 9), List.of());

    assertEquals(75, entidad.calcularPuntajeCompatibilidad(donacion));
    assertEquals(1, candidatos.size());
    assertEquals(75, candidatos.get(0).getPuntaje());
  }
}
