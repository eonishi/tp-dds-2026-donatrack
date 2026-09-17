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
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DonacionesRepositoryTest implements SimplePersistenceTest {

  private Donante donante;
  private SubCategoria subCategoria;
  private EntidadBeneficiaria entidadBeneficiaria;

  @BeforeEach
  void persistPrerequisites() {
    donante = crearDonanteDummy();
    subCategoria = new SubCategoria("X", Categoria.ALIMENTO);
    entidadBeneficiaria = new EntidadBeneficiaria("Ent", "Dir", "Tel");
    // Donacion cascadea a su Bien y a su HistorialEstado, pero no al donante, a la subcategoría de
    // cada bien ni a la entidad beneficiaria: esos ya deben estar almacenados al persistir una
    // donación.
    persist(donante);
    persist(subCategoria);
    persist(entidadBeneficiaria);
  }

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

  private Bien crearBienDummy() {
    return new Bien(
        "Test", "", 1.0, UnidadMedida.UNIDADES, null, EstadoUso.NO_APLICA, subCategoria);
  }

  private Donacion crearDonacionConEstado(NombreEstado estado) {
    Donacion donacion = new Donacion(donante, List.of(crearBienDummy()));
    // Force the internal estado field via reflection or state machine walk
    // Walk: EnDeposito → asignar → AsignacionRealizada
    if (estado == NombreEstado.EN_DEPOSITO) {
      return donacion; // default state
    }
    if (estado == NombreEstado.ASIGNACION_REALIZADA) {
      donacion.confirmarEntidadBeneficiaria(entidadBeneficiaria);
      return donacion;
    }
    throw new IllegalArgumentException("Estado no soportado en helper: " + estado);
  }

  @Test
  void getDonacionesEnAsignacionRealizada_ConMezcla_DebeFiltrarCorrectamente() {
    DonacionesRepository repo = new DonacionesRepository();
    Donacion enDeposito = crearDonacionConEstado(NombreEstado.EN_DEPOSITO);
    Donacion asignada1 = crearDonacionConEstado(NombreEstado.ASIGNACION_REALIZADA);
    Donacion asignada2 = crearDonacionConEstado(NombreEstado.ASIGNACION_REALIZADA);
    repo.addDonacion(enDeposito);
    repo.addDonacion(asignada1);
    repo.addDonacion(asignada2);

    List<Donacion> resultado = repo.getDonacionesEnAsignacionRealizada();

    assertEquals(2, resultado.size());
    assertTrue(resultado.contains(asignada1));
    assertTrue(resultado.contains(asignada2));
  }

  @Test
  void getDonacionesEnAsignacionRealizada_SinCoincidencias_DebeRetornarListaVacia() {
    DonacionesRepository repo = new DonacionesRepository();
    Donacion enDeposito = crearDonacionConEstado(NombreEstado.EN_DEPOSITO);
    repo.addDonacion(enDeposito);

    List<Donacion> resultado = repo.getDonacionesEnAsignacionRealizada();

    assertTrue(resultado.isEmpty());
  }

  @Test
  void getDonacionesEnAsignacionRealizada_DevuelveListaInmodificable() {
    DonacionesRepository repo = new DonacionesRepository();
    repo.addDonacion(crearDonacionConEstado(NombreEstado.ASIGNACION_REALIZADA));

    List<Donacion> resultado = repo.getDonacionesEnAsignacionRealizada();

    assertThrows(UnsupportedOperationException.class, () -> resultado.add(null));
  }

  @Test
  void registeredDonations_RoundTripThroughTheDatabase() {
    DonacionesRepository repo = new DonacionesRepository();
    repo.addDonacion(crearDonacionConEstado(NombreEstado.EN_DEPOSITO));
    repo.addDonacion(crearDonacionConEstado(NombreEstado.ASIGNACION_REALIZADA));
    entityManager().flush();
    entityManager().clear();

    DonacionesRepository reloaded = new DonacionesRepository();

    assertEquals(
        2,
        reloaded.getDonaciones().size(),
        "Las donaciones registradas deben leerse de la base, no de una colección en memoria");
    List<Donacion> enDeposito = reloaded.getDonacionesEnDeposito();
    List<Donacion> asignadas = reloaded.getDonacionesEnAsignacionRealizada();
    assertEquals(1, enDeposito.size(), "Una donación en depósito debe recuperarse como tal");
    assertEquals(1, asignadas.size(), "Una donación asignada debe recuperarse como asignada");
    // Tras limpiar el entity manager las instancias son nuevas, así que la donación recargada se
    // compara por sus datos de negocio.
    Donacion recargada = asignadas.get(0);
    assertEquals(
        subCategoria.getNombre(),
        recargada.getSubcategoria().getNombre(),
        "La donación recargada debe conservar su subcategoría");
    assertEquals(
        "Test",
        recargada.getBienes().get(0).getDescripcion(),
        "La donación recargada debe conservar su bien");
  }

  @Test
  void getDonacionesEnDeposito_ReflejaLaUltimaTransicion() {
    DonacionesRepository repo = new DonacionesRepository();
    Donacion donacion = crearDonacionConEstado(NombreEstado.EN_DEPOSITO);
    repo.addDonacion(donacion);

    assertTrue(
        repo.getDonacionesEnDeposito().contains(donacion),
        "Una donación recién registrada debe estar en depósito");

    // La donación ahora tiene dos entradas de historial: la inicial EN_DEPOSITO y esta
    // ASIGNACION_REALIZADA. Solo la última decide el estado.
    donacion.asignar();
    entityManager().flush();
    entityManager().clear();

    DonacionesRepository reloaded = new DonacionesRepository();

    assertTrue(
        reloaded.getDonacionesEnDeposito().isEmpty(),
        "Una donación asignada no debe seguir apareciendo en depósito");
    List<Donacion> asignadas = reloaded.getDonacionesEnAsignacionRealizada();
    assertEquals(
        1, asignadas.size(), "La donación asignada debe aparecer como asignación realizada");
    assertEquals(
        NombreEstado.ASIGNACION_REALIZADA,
        asignadas.get(0).getNombreEstado(),
        "El estado derivado debe ser la última transición");
  }

  @Test
  void contarDonacionesAsignadas_CuentaLasAsignadasDeLaEntidad() {
    DonacionesRepository repo = new DonacionesRepository();
    repo.addDonacion(crearDonacionConEstado(NombreEstado.EN_DEPOSITO));
    repo.addDonacion(crearDonacionConEstado(NombreEstado.ASIGNACION_REALIZADA));
    // Asignada y luego entregada: dos transiciones que califican para una única donación.
    Donacion entregada = crearDonacionConEstado(NombreEstado.EN_DEPOSITO);
    entregada.confirmarEntidadBeneficiaria(entidadBeneficiaria);
    entregada.planificarRuta();
    entregada.iniciarTraslado();
    entregada.confirmarEntrega();
    repo.addDonacion(entregada);

    long asignadas = repo.contarDonacionesAsignadas(entidadBeneficiaria, LocalDate.now());

    assertEquals(
        2, asignadas, "Cada donación debe contarse una sola vez, aunque tenga varias transiciones");
  }
}
