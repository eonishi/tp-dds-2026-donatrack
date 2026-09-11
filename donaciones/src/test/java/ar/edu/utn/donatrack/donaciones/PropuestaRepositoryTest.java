package ar.edu.utn.donatrack.donaciones;

import static ar.edu.utn.donatrack.testsupport.FabricaDePrueba.nuevaDonacion;
import static ar.edu.utn.donatrack.testsupport.FabricaDePrueba.nuevaEntidad;
import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PropuestaRepositoryTest {

  private PropuestaRepository repository;
  private Donacion donacion;
  private PropuestaDeAsignacion propuesta;

  @BeforeEach
  void setUp() {
    repository = new PropuestaRepositoryImpl();
    donacion = crearDonacionDummy();
    propuesta = new PropuestaDeAsignacion(donacion, crearRankingsDummy());
  }

  @Test
  void saveYFindPendingByDonacion_RetornaPropuestaPendiente() {
    repository.save(propuesta);

    var encontrada = repository.findPendingByDonacion(donacion);

    assertTrue(encontrada.isPresent(), "Debe encontrar la propuesta guardada");
    assertTrue(encontrada.get().isPendiente(), "La propuesta debe estar pendiente");
    assertSame(propuesta, encontrada.get(), "Debe ser la misma propuesta");
  }

  @Test
  void findPendingByDonacion_PropuestaConfirmada_RetornaEmpty() {
    repository.save(propuesta);
    propuesta.confirmar(nuevaEntidad("Hogar"));

    var encontrada = repository.findPendingByDonacion(donacion);

    assertTrue(encontrada.isEmpty(), "Propuesta confirmada no debe aparecer como pendiente");
  }

  @Test
  void findAllPending_RetornaSoloPendientes() {
    Donacion donacionB = crearDonacionDummy();
    PropuestaDeAsignacion propuestaB = new PropuestaDeAsignacion(donacionB, crearRankingsDummy());

    repository.save(propuesta);
    repository.save(propuestaB);

    // Confirmar la primera
    propuesta.confirmar(nuevaEntidad("Hogar"));

    List<PropuestaDeAsignacion> pendientes = repository.findAllPending();

    assertEquals(1, pendientes.size(), "Solo debe haber una propuesta pendiente");
    assertSame(propuestaB, pendientes.get(0), "Debe ser la que no se confirmó");
  }

  @Test
  void findAllPending_SinPropuestas_RetornaListaVacia() {
    List<PropuestaDeAsignacion> pendientes = repository.findAllPending();
    assertTrue(pendientes.isEmpty(), "Sin propuestas debe retornar lista vacía");
  }

  @Test
  void remove_EliminaPropuesta() {
    repository.save(propuesta);
    repository.remove(donacion);

    var encontrada = repository.findPendingByDonacion(donacion);
    assertTrue(encontrada.isEmpty(), "Propuesta eliminada no debe ser encontrada");
  }

  @Test
  void save_UpsertReemplazaPropuestaExistente() {
    repository.save(propuesta);

    PropuestaDeAsignacion reemplazo = new PropuestaDeAsignacion(donacion, crearRankingsDummy());
    repository.save(reemplazo);

    var encontrada = repository.findPendingByDonacion(donacion);
    assertTrue(encontrada.isPresent());
    assertSame(reemplazo, encontrada.get(), "Debe devolver la última propuesta guardada");
  }

  // ── Helpers ────────────────────────────────────────────────────────

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

  private static Donacion crearDonacionDummy() {
    Bien bien =
        new Bien(
            "Test",
            "",
            1.0,
            UnidadMedida.UNIDADES,
            null,
            EstadoUso.NO_APLICA,
            new SubCategoria("X", Categoria.ALIMENTO));
    return nuevaDonacion(crearDonanteDummy(), List.of(bien));
  }

  private static List<RankingMatchmaking> crearRankingsDummy() {
    EntidadBeneficiaria entidad = nuevaEntidad("Entidad");
    RankingMatchmaking ranking1 =
        new RankingMatchmaking("algoritmo-1", List.of(new MatchCandidato(entidad, 50)));
    RankingMatchmaking ranking2 =
        new RankingMatchmaking("algoritmo-2", List.of(new MatchCandidato(entidad, 80)));
    return List.of(ranking1, ranking2);
  }
}
