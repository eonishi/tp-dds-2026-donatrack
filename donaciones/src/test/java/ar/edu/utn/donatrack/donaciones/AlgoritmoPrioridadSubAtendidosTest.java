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
import ar.edu.utn.donatrack.estados.Entregada;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlgoritmoPrioridadSubAtendidosTest {

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
  void rankear_ConMasDe10Entidades_RetornaMaximo10Candidatos() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Donacion donacion =
        new Donacion(
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

    // Create 15 entities with matching subcategoria and no recepciones (all get score 100)
    var entidades =
        java.util.stream.IntStream.range(0, 15)
            .mapToObj(
                i -> {
                  EntidadBeneficiaria e = new EntidadBeneficiaria("Entidad " + i, "Dir", "Tel");
                  e.agregarNecesidadExtraordinaria(
                      new Necesidad(fideos, 5.0, UnidadMedida.KILOGRAMOS, "Fideos"));
                  return e;
                })
            .toList();

    var ranking =
        new AlgoritmoPrioridadSubAtendidos(new DonacionesRepository())
            .rankear(donacion, entidades, LocalDate.of(2026, 7, 11));

    assertTrue(
        ranking.getCandidatos().size() <= 10,
        "Prioridad sub-atendidos debe retornar máximo 10 candidatos");
    assertEquals(
        10,
        ranking.getCandidatos().size(),
        "Con 15 entidades sub-atendidas debe retornar exactamente 10");
    // Verify sort order: highest puntaje first
    for (int i = 1; i < ranking.getCandidatos().size(); i++) {
      assertTrue(
          ranking.getCandidatos().get(i - 1).getPuntaje()
              >= ranking.getCandidatos().get(i).getPuntaje(),
          "Candidatos deben estar ordenados por puntaje descendente");
    }
  }

  @Test
  void rankear_ConMenosDe10Entidades_RetornaTodas() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Donacion donacion =
        new Donacion(
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

    EntidadBeneficiaria e1 = new EntidadBeneficiaria("Comedor A", "Dir", "Tel");
    e1.agregarNecesidadExtraordinaria(
        new Necesidad(fideos, 5.0, UnidadMedida.KILOGRAMOS, "Fideos"));
    EntidadBeneficiaria e2 = new EntidadBeneficiaria("Comedor B", "Dir", "Tel");
    e2.agregarNecesidadExtraordinaria(
        new Necesidad(fideos, 3.0, UnidadMedida.KILOGRAMOS, "Fideos"));
    EntidadBeneficiaria e3 = new EntidadBeneficiaria("Comedor C", "Dir", "Tel");
    e3.agregarNecesidadExtraordinaria(
        new Necesidad(fideos, 8.0, UnidadMedida.KILOGRAMOS, "Fideos"));

    var ranking =
        new AlgoritmoPrioridadSubAtendidos(new DonacionesRepository())
            .rankear(donacion, List.of(e1, e2, e3), LocalDate.of(2026, 7, 11));

    assertEquals(3, ranking.getCandidatos().size(), "Con 3 entidades debe retornar las 3");
  }

  @Test
  void rankear_ConEntidadesSinNecesidadCompatible_RetornaVacio() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    SubCategoria sillas = new SubCategoria("Sillas", Categoria.MOBILIARIO);
    Donacion donacion =
        new Donacion(
            crearDonanteDummy(),
            List.of(
                new Bien("Silla", "", 1.0, UnidadMedida.UNIDADES, null, EstadoUso.NUEVO, sillas)));

    EntidadBeneficiaria entidad = new EntidadBeneficiaria("Comedor", "Dir", "Tel");
    entidad.agregarNecesidadExtraordinaria(
        new Necesidad(fideos, 5.0, UnidadMedida.KILOGRAMOS, "Fideos"));

    var ranking =
        new AlgoritmoPrioridadSubAtendidos(new DonacionesRepository())
            .rankear(donacion, List.of(entidad), LocalDate.of(2026, 7, 11));

    assertTrue(
        ranking.getCandidatos().isEmpty(),
        "Sin entidades con necesidad compatible debe retornar vacío");
  }

  @Test
  void puntaje_EntidadConMuchasEntregas_RetornaPuntajeMenor() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Donacion donacion =
        new Donacion(
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

    EntidadBeneficiaria sinRecepciones = new EntidadBeneficiaria("Sin Recibir", "Dir", "Tel");
    sinRecepciones.agregarNecesidadExtraordinaria(
        new Necesidad(fideos, 5.0, UnidadMedida.KILOGRAMOS, "Fideos"));

    EntidadBeneficiaria conMuchas = new EntidadBeneficiaria("Ya Recibio", "Dir", "Tel");
    conMuchas.agregarNecesidadExtraordinaria(
        new Necesidad(fideos, 5.0, UnidadMedida.KILOGRAMOS, "Fideos"));
    // Simulate 50 entregas
    DonacionesRepository donacionesRepository = new DonacionesRepository();
    for (int i = 0; i < 50; i++) {
      donacionesRepository.addDonacion(
          crearDonacionEntregada(conMuchas, fideos, LocalDateTime.of(2026, 6, 1, 10, 0)));
    }

    var ranking =
        new AlgoritmoPrioridadSubAtendidos(donacionesRepository)
            .rankear(donacion, List.of(sinRecepciones, conMuchas), LocalDate.of(2026, 7, 11));

    assertEquals(2, ranking.getCandidatos().size());
    assertTrue(
        ranking.getCandidatos().get(0).getPuntaje() > ranking.getCandidatos().get(1).getPuntaje(),
        "Entidad sin entregas debe tener mayor puntaje que la que ya recibió mucho");
    assertEquals(
        100,
        ranking.getCandidatos().get(0).getPuntaje(),
        "Entidad sin entregas debería tener puntaje 100");
    assertEquals(
        50,
        ranking.getCandidatos().get(1).getPuntaje(),
        "Entidad con 50 entregas debería tener puntaje 100 - 50 = 50");
  }

  private static Donacion crearDonacionEntregada(
      EntidadBeneficiaria entidad, SubCategoria subCategoria, LocalDateTime fechaEntrega) {
    Donacion donacion =
        new Donacion(
            crearDonanteDummy(),
            List.of(
                new Bien(
                    "Entregada",
                    "",
                    1.0,
                    UnidadMedida.KILOGRAMOS,
                    null,
                    EstadoUso.NO_APLICA,
                    subCategoria)));
    donacion.confirmarEntidadBeneficiaria(entidad);
    donacion.cambiarEstado(
        new HistorialEstado(fechaEntrega, NombreEstado.ENTREGADA, null), new Entregada());
    return donacion;
  }
}
