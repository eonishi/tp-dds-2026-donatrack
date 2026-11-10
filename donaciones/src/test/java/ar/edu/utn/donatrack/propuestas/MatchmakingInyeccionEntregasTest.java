package ar.edu.utn.donatrack.propuestas;

import static ar.edu.utn.donatrack.testsupport.FabricaDePrueba.notificacionesDescartadas;
import static ar.edu.utn.donatrack.testsupport.FabricaDePrueba.nuevaDonacion;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.donatrack.beneficiarios.BeneficiariosRepository;
import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.beneficiarios.Necesidad;
import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.bienes.Categoria;
import ar.edu.utn.donatrack.bienes.EstadoUso;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.bienes.UnidadMedida;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.DonacionesRepository;
import ar.edu.utn.donatrack.donaciones.EstadoDonacion;
import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.Genero;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.TipoDocumento;
import ar.edu.utn.donatrack.propuestas.matchmaking.AlgoritmoMatchmaking;
import ar.edu.utn.donatrack.propuestas.matchmaking.MatchCandidato;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Verifica que el caso de uso le pase al algoritmo las donaciones entregadas que leyó, sin que el
 * algoritmo conozca el repositorio de donaciones.
 */
class MatchmakingInyeccionEntregasTest {

  private static final LocalDate FECHA_REFERENCIA = LocalDate.of(2026, 7, 6);

  @Test
  void ejecutarMatchmakingLePasaLasEntregadasDelRepositorioAlRanking() {
    SubCategoria alimentos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Donante donante = crearDonanteDummy();
    Donacion donacion = nuevaDonacion(donante, List.of(bienDe(alimentos)));
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(1L, "Hogar", "Direccion", "Telefono");
    entidad.agregarNecesidadExtraordinaria(
        new Necesidad(alimentos, 5, UnidadMedida.KILOGRAMOS, "Fideos"));
    Donacion entregada = crearDonacionEntregada(donante, entidad, alimentos);

    DonacionesRepository donaciones = mock(DonacionesRepository.class);
    when(donaciones.getDonacionesEntregadas()).thenReturn(List.of(entregada));
    BeneficiariosRepository beneficiarios = mock(BeneficiariosRepository.class);
    when(beneficiarios.getBeneficiarios()).thenReturn(Set.of(entidad));
    AlgoritmoRegistrador algoritmo = new AlgoritmoRegistrador(entidad);
    ServicioMatchmakingDonaciones servicio =
        new ServicioMatchmakingDonaciones(
            donaciones, beneficiarios, mock(PropuestaRepository.class), List.of(algoritmo));

    PropuestaDeAsignacion propuesta =
        servicio.ejecutarMatchmakingParaDonacion(donacion, FECHA_REFERENCIA);

    assertEquals(
        List.of(entregada),
        algoritmo.entregadasAlRankear,
        "El algoritmo debe recibir las entregadas del repositorio al rankear");
    assertFalse(
        propuesta.getCandidatos().isEmpty(),
        "La propuesta debe conservar los candidatos rankeados");
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

  private static Bien bienDe(SubCategoria subCategoria) {
    return new Bien(
        "Fideos", "", 10.0, UnidadMedida.KILOGRAMOS, null, EstadoUso.NO_APLICA, subCategoria);
  }

  private static Donacion crearDonacionEntregada(
      Donante donante, EntidadBeneficiaria entidad, SubCategoria subCategoria) {
    Donacion donacion = nuevaDonacion(donante, List.of(bienDe(subCategoria)));
    donacion.confirmarEntidadBeneficiaria(entidad, notificacionesDescartadas());
    donacion.cambiarEstado(EstadoDonacion.ENTREGADA, null);
    return donacion;
  }

  /**
   * Algoritmo de prueba que registra las entregadas que recibe al rankear.
   *
   * <p>No consulta repositorios: solo expone el parámetro del ranking para que el test lo compare.
   */
  private static final class AlgoritmoRegistrador extends AlgoritmoMatchmaking {
    private final EntidadBeneficiaria entidad;
    private List<Donacion> entregadasAlRankear;

    private AlgoritmoRegistrador(EntidadBeneficiaria entidad) {
      this.entidad = entidad;
    }

    @Override
    public String getNombre() {
      return "registrador";
    }

    @Override
    protected List<MatchCandidato> ordenarCandidatos(
        Donacion donacion,
        Collection<EntidadBeneficiaria> entidadesBeneficiarias,
        LocalDate fechaReferencia,
        Collection<Donacion> donacionesEntregadas) {
      this.entregadasAlRankear = List.copyOf(donacionesEntregadas);
      return List.of(new MatchCandidato(entidad, 100));
    }
  }
}
