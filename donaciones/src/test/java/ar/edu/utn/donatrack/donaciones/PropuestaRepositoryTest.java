package ar.edu.utn.donatrack.donaciones;

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
import ar.edu.utn.donatrack.propuestas.PropuestaDeAsignacion;
import ar.edu.utn.donatrack.propuestas.PropuestaRepository;
import ar.edu.utn.donatrack.propuestas.PropuestaRepositoryImpl;
import ar.edu.utn.donatrack.propuestas.matchmaking.MatchCandidato;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PropuestaRepositoryTest implements SimplePersistenceTest {

  private PropuestaRepository repository;
  private Donante donante;
  private SubCategoria subCategoria;
  private EntidadBeneficiaria entidad;
  private Donacion donacion;
  private PropuestaDeAsignacion propuesta;

  @BeforeEach
  void setUp() {
    repository = new PropuestaRepositoryImpl();
    donante = crearDonanteDummy();
    subCategoria = new SubCategoria("X", Categoria.ALIMENTO);
    entidad = new EntidadBeneficiaria("Entidad", "Dir", "Tel");
    persist(donante);
    persist(subCategoria);
    persist(entidad);
    donacion = crearDonacionDummy();
    persist(donacion);
    propuesta = new PropuestaDeAsignacion(donacion, crearCandidatosDummy(entidad, 50, 80));
  }

  @Test
  void saveYFindPendingByDonacion_PersisteYRecuperaDesdeLaBase() {
    repository.save(propuesta);
    entityManager().flush();
    entityManager().clear();

    var encontrada = new PropuestaRepositoryImpl().findPendingByDonacion(donacion);

    assertTrue(encontrada.isPresent(), "Debe encontrar la propuesta guardada en la base");
    assertTrue(encontrada.get().isPendiente(), "La propuesta debe estar pendiente");
    assertEquals(donacion.getId(), encontrada.get().getDonacion().getId());
    assertEquals(1, encontrada.get().getCandidatos().size());
    assertEquals(65, encontrada.get().getCandidatos().get(0).getPuntaje());
  }

  @Test
  void findPendingByDonacion_PropuestaConfirmada_RetornaEmpty() {
    repository.save(propuesta);
    propuesta.confirmar(candidata(propuesta));
    entityManager().flush();
    entityManager().clear();

    var encontrada = repository.findPendingByDonacion(donacion);

    assertTrue(encontrada.isEmpty(), "Propuesta confirmada no debe aparecer como pendiente");
  }

  @Test
  void findAllPending_RetornaSoloPendientes() {
    Donacion donacionB = crearDonacionDummy();
    persist(donacionB);
    PropuestaDeAsignacion propuestaB =
        new PropuestaDeAsignacion(donacionB, crearCandidatosDummy(entidad, 50, 80));

    repository.save(propuesta);
    repository.save(propuestaB);

    // Confirmar la primera
    propuesta.confirmar(candidata(propuesta));
    entityManager().flush();
    entityManager().clear();

    List<PropuestaDeAsignacion> pendientes = repository.findAllPending();

    assertEquals(1, pendientes.size(), "Solo debe haber una propuesta pendiente");
    assertEquals(
        donacionB.getId(),
        pendientes.get(0).getDonacion().getId(),
        "Debe ser la que no se confirmó");
  }

  @Test
  void findAllPending_SinPropuestas_RetornaListaVacia() {
    List<PropuestaDeAsignacion> pendientes = repository.findAllPending();
    assertTrue(pendientes.isEmpty(), "Sin propuestas debe retornar lista vacía");
  }

  @Test
  void remove_EliminaPropuesta() {
    repository.save(propuesta);
    entityManager().flush();

    repository.remove(donacion);
    entityManager().flush();
    entityManager().clear();

    var encontrada = repository.findPendingByDonacion(donacion);
    assertTrue(encontrada.isEmpty(), "Propuesta eliminada no debe ser encontrada");
  }

  @Test
  void save_ActualizaListaDeCandidatosDePropuestaExistente() {
    repository.save(propuesta);

    EntidadBeneficiaria otraEntidad = new EntidadBeneficiaria("Otra Entidad", "Dir 2", "Tel 2");
    persist(otraEntidad);
    PropuestaDeAsignacion reemplazo =
        new PropuestaDeAsignacion(donacion, crearCandidatosDummy(otraEntidad, 90, 100));
    repository.save(reemplazo);
    entityManager().flush();
    entityManager().clear();

    var encontrada = repository.findPendingByDonacion(donacion);
    assertTrue(encontrada.isPresent());
    assertEquals(1, encontrada.get().getCandidatos().size());
    assertEquals(
        otraEntidad.getId(),
        encontrada.get().getCandidatos().get(0).getEntidadBeneficiaria().getId(),
        "Debe actualizar los candidatos sobre la propuesta de la donación");
    assertEquals(95, encontrada.get().getCandidatos().get(0).getPuntaje());
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

  private Donacion crearDonacionDummy() {
    Bien bien =
        new Bien("Test", "", 1.0, UnidadMedida.UNIDADES, null, EstadoUso.NO_APLICA, subCategoria);
    return new Donacion(donante, List.of(bien));
  }

  private static List<List<MatchCandidato>> crearCandidatosDummy(
      EntidadBeneficiaria entidad, int puntajeA, int puntajeB) {
    return List.of(
        List.of(new MatchCandidato(entidad, puntajeA)),
        List.of(new MatchCandidato(entidad, puntajeB)));
  }

  private static EntidadBeneficiaria candidata(PropuestaDeAsignacion propuesta) {
    return propuesta.getCandidatos().get(0).getEntidadBeneficiaria();
  }
}
