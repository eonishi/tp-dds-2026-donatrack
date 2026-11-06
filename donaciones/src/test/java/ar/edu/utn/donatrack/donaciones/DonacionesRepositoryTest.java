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
    entidadBeneficiaria = nuevaEntidad("Ent");
    // Donacion cascadea a su Bien y guarda su HistorialEstado, pero no cascadea al donante, a la
    // subcategoría de cada bien ni a la entidad beneficiaria: esos ya deben estar almacenados al
    // persistir una donación.
    persist(donante);
    persist(subCategoria);
    persist(entidadBeneficiaria);
  }

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

  private Bien crearBienDummy() {
    return new Bien(
        "Test", "", 1.0, UnidadMedida.UNIDADES, null, EstadoUso.NO_APLICA, subCategoria);
  }

  private Donacion crearDonacionConEstado(EstadoDonacion estado) {
    Donacion donacion = nuevaDonacion(donante, List.of(crearBienDummy()));
    // Force the internal estado field via reflection or state machine walk
    // Walk: EnDeposito → asignar → AsignacionRealizada
    if (estado == EstadoDonacion.EN_DEPOSITO) {
      return donacion; // default state
    }
    if (estado == EstadoDonacion.ASIGNACION_REALIZADA) {
      donacion.confirmarEntidadBeneficiaria(entidadBeneficiaria);
      return donacion;
    }
    if (estado == EstadoDonacion.ENTREGADA) {
      donacion.confirmarEntidadBeneficiaria(entidadBeneficiaria);
      donacion.cambiarEstado(EstadoDonacion.ENTREGADA, null);
      return donacion;
    }
    throw new IllegalArgumentException("Estado no soportado en helper: " + estado);
  }

  @Test
  void getDonacionesEnAsignacionRealizada_ConMezcla_DebeFiltrarCorrectamente() {
    DonacionesRepository repo = new DonacionesRepository();
    Donacion enDeposito = crearDonacionConEstado(EstadoDonacion.EN_DEPOSITO);
    Donacion asignada1 = crearDonacionConEstado(EstadoDonacion.ASIGNACION_REALIZADA);
    Donacion asignada2 = crearDonacionConEstado(EstadoDonacion.ASIGNACION_REALIZADA);
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
    Donacion enDeposito = crearDonacionConEstado(EstadoDonacion.EN_DEPOSITO);
    repo.addDonacion(enDeposito);

    List<Donacion> resultado = repo.getDonacionesEnAsignacionRealizada();

    assertTrue(resultado.isEmpty());
  }

  @Test
  void getDonacionesEntregadas_ConMezcla_DebeRetornarSoloLasEntregadas() {
    DonacionesRepository repo = new DonacionesRepository();
    Donacion enDeposito = crearDonacionConEstado(EstadoDonacion.EN_DEPOSITO);
    Donacion asignada = crearDonacionConEstado(EstadoDonacion.ASIGNACION_REALIZADA);
    Donacion entregada1 = crearDonacionConEstado(EstadoDonacion.ENTREGADA);
    Donacion entregada2 = crearDonacionConEstado(EstadoDonacion.ENTREGADA);
    repo.addDonacion(enDeposito);
    repo.addDonacion(asignada);
    repo.addDonacion(entregada1);
    repo.addDonacion(entregada2);

    List<Donacion> resultado = repo.getDonacionesEntregadas();

    assertEquals(2, resultado.size());
    assertTrue(resultado.contains(entregada1));
    assertTrue(resultado.contains(entregada2));
    assertFalse(resultado.contains(enDeposito));
    assertFalse(resultado.contains(asignada));
  }

  @Test
  void getDonacionesEnAsignacionRealizada_DevuelveListaInmodificable() {
    DonacionesRepository repo = new DonacionesRepository();
    repo.addDonacion(crearDonacionConEstado(EstadoDonacion.ASIGNACION_REALIZADA));

    List<Donacion> resultado = repo.getDonacionesEnAsignacionRealizada();

    assertThrows(UnsupportedOperationException.class, () -> resultado.add(null));
  }

  @Test
  void unaDonacionConVariasEntregasEnLaVentanaCuentaUnaSolaVez() {
    DonacionesRepository repo = new DonacionesRepository();
    Donacion entregada = crearDonacionConEstado(EstadoDonacion.ASIGNACION_REALIZADA);
    entregada.cambiarEstado(EstadoDonacion.ENTREGADA, null);
    entregada.cambiarEstado(EstadoDonacion.ENTREGADA, null);
    repo.addDonacion(entregada);
    repo.addDonacion(crearDonacionConEstado(EstadoDonacion.ASIGNACION_REALIZADA));

    assertEquals(
        1,
        repo.getDonacionesEntregadas().stream()
            .filter(d -> d.fueEntregadaA(entidadBeneficiaria, LocalDate.now()))
            .count());
  }

  @Test
  void lasEntregasAnterioresALaVentanaNoCuentan() {
    DonacionesRepository repo = new DonacionesRepository();
    Donacion entregada = crearDonacionConEstado(EstadoDonacion.ASIGNACION_REALIZADA);
    entregada.cambiarEstado(EstadoDonacion.ENTREGADA, null);
    repo.addDonacion(entregada);

    assertEquals(
        0,
        repo.getDonacionesEntregadas().stream()
            .filter(d -> d.fueEntregadaA(entidadBeneficiaria, LocalDate.now().plusDays(1)))
            .count());
  }
}
