package ar.edu.utn.donatrack.donaciones;

import static ar.edu.utn.donatrack.testsupport.FabricaDePrueba.nuevaDonacion;
import static ar.edu.utn.donatrack.testsupport.FabricaDePrueba.nuevaEntidad;
import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiariaId;
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
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlgoritmoPrioridadSubAtendidosTest implements SimplePersistenceTest {

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

    // Create 15 entities with matching subcategoria and no entregas (all get score 100)
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

    // El ranking real consulta las entregas por entidad: las subcategorías de las necesidades y
    // las entidades deben estar almacenadas.
    persist(fideos);
    entidades.forEach(this::persist);

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

    EntidadBeneficiaria e1 = nuevaEntidad("Comedor A");
    e1.agregarNecesidadExtraordinaria(
        new Necesidad(fideos, 5.0, UnidadMedida.KILOGRAMOS, "Fideos"));
    EntidadBeneficiaria e2 = nuevaEntidad("Comedor B");
    e2.agregarNecesidadExtraordinaria(
        new Necesidad(fideos, 3.0, UnidadMedida.KILOGRAMOS, "Fideos"));
    EntidadBeneficiaria e3 = nuevaEntidad("Comedor C");
    e3.agregarNecesidadExtraordinaria(
        new Necesidad(fideos, 8.0, UnidadMedida.KILOGRAMOS, "Fideos"));
    persist(fideos);
    persist(e1);
    persist(e2);
    persist(e3);

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
        nuevaDonacion(
            crearDonanteDummy(),
            List.of(
                new Bien("Silla", "", 1.0, UnidadMedida.UNIDADES, null, EstadoUso.NUEVO, sillas)));

    EntidadBeneficiaria entidad = nuevaEntidad("Comedor");
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

    EntidadBeneficiaria sinEntregas = nuevaEntidad("Sin Recibir");
    sinEntregas.agregarNecesidadExtraordinaria(
        new Necesidad(fideos, 5.0, UnidadMedida.KILOGRAMOS, "Fideos"));

    EntidadBeneficiaria conMuchas = nuevaEntidad("Ya Recibio");
    conMuchas.agregarNecesidadExtraordinaria(
        new Necesidad(fideos, 5.0, UnidadMedida.KILOGRAMOS, "Fideos"));
    // Simulate 50 entregas
    // El ranking real consulta las entregas por entidad: el donante, la subcategoría de la
    // necesidad y las entidades deben estar almacenados.
    Donante donante = crearDonanteDummy();
    persist(fideos);
    persist(donante);
    persist(sinEntregas);
    persist(conMuchas);
    DonacionesRepository donacionesRepository = new DonacionesRepository();
    for (int i = 0; i < 50; i++) {
      donacionesRepository.addDonacion(
          crearDonacionEntregada(donante, conMuchas, fideos, LocalDateTime.of(2026, 6, 1, 10, 0)));
    }

    var ranking =
        new AlgoritmoPrioridadSubAtendidos(donacionesRepository)
            .rankear(donacion, List.of(sinEntregas, conMuchas), LocalDate.of(2026, 7, 11));

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

  @Test
  void rankear_ConEntidadesHomonimasEmpatadas_DesempataPorId() {
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

    EntidadBeneficiaria mayor =
        new EntidadBeneficiaria(new EntidadBeneficiariaId(2L), "Comedor Los Pibes", "Dir", "Tel");
    EntidadBeneficiaria menor =
        new EntidadBeneficiaria(new EntidadBeneficiariaId(1L), "Comedor Los Pibes", "Dir", "Tel");
    for (EntidadBeneficiaria entidad : List.of(mayor, menor)) {
      entidad.agregarNecesidadExtraordinaria(
          new Necesidad(fideos, 5.0, UnidadMedida.KILOGRAMOS, "Fideos"));
    }
    persist(fideos);
    persist(mayor);
    persist(menor);

    var ranking =
        new AlgoritmoPrioridadSubAtendidos(new DonacionesRepository())
            .rankear(donacion, List.of(mayor, menor), LocalDate.of(2026, 7, 11));

    assertEquals(2, ranking.getCandidatos().size());
    assertEquals(
        menor.getId(),
        ranking.getCandidatos().get(0).getEntidadBeneficiaria().getId(),
        "Con puntaje y razón social empatados debe desempatar por id");
  }

  private static Donacion crearDonacionEntregada(
      Donante donante,
      EntidadBeneficiaria entidad,
      SubCategoria subCategoria,
      LocalDateTime fechaEntrega) {
    Donacion donacion =
        nuevaDonacion(
            donante,
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
        new HistorialEstado(fechaEntrega, EstadoDonacion.ENTREGADA, null),
        EstadoDonacion.ENTREGADA);
    return donacion;
  }
}
