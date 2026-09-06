package ar.edu.utn.donatrack.donaciones;

import static org.junit.jupiter.api.Assertions.*;

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
import org.junit.jupiter.api.Test;

class DonacionesRepositoryTest {

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

  private static Bien crearBienDummy() {
    return new Bien(
        "Test",
        "",
        1.0,
        UnidadMedida.UNIDADES,
        null,
        EstadoUso.NO_APLICA,
        new SubCategoria("X", Categoria.ALIMENTO));
  }

  private Donacion crearDonacionConEstado(NombreEstado estado) {
    Donacion donacion = new Donacion(crearDonanteDummy(), List.of(crearBienDummy()));
    // Force the internal estado field via reflection or state machine walk
    // Walk: EnDeposito → asignar → AsignacionRealizada
    if (estado == NombreEstado.EN_DEPOSITO) {
      return donacion; // default state
    }
    if (estado == NombreEstado.ASIGNACION_REALIZADA) {
      donacion.confirmarEntidadBeneficiaria(
          new ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria("Ent", "Dir", "Tel"));
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
}
