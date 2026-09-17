package ar.edu.utn.donatrack;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ar.edu.utn.donatrack.beneficiarios.BeneficiariosRepository;
import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.beneficiarios.Necesidad;
import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.bienes.Categoria;
import ar.edu.utn.donatrack.bienes.EstadoUso;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.bienes.UnidadMedida;
import ar.edu.utn.donatrack.donaciones.AlgoritmoCompatibilidadSemantica;
import ar.edu.utn.donatrack.donaciones.AlgoritmoMatchmaking;
import ar.edu.utn.donatrack.donaciones.AlgoritmoPrioridadSubAtendidos;
import ar.edu.utn.donatrack.donaciones.CargaDonacion;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.DonacionesRepository;
import ar.edu.utn.donatrack.donaciones.MatchCandidato;
import ar.edu.utn.donatrack.donaciones.MatchmakingScheduler;
import ar.edu.utn.donatrack.donaciones.PropuestaDeAsignacion;
import ar.edu.utn.donatrack.donaciones.PropuestaRepository;
import ar.edu.utn.donatrack.donaciones.PropuestaRepositoryImpl;
import ar.edu.utn.donatrack.donaciones.RankingMatchmaking;
import ar.edu.utn.donatrack.donaciones.ServicioMatchmakingDonaciones;
import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.Genero;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.TipoDocumento;
import ar.edu.utn.donatrack.estados.AsignacionRealizada;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

public class MatchmakingDonacionesTest {

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
  void cargaDonacionRegistraLasDonacionesEnElRepositorio() {
    SubCategoria subCategoria = new SubCategoria("Sillas", Categoria.MOBILIARIO);
    Bien silla =
        new Bien("Silla", "", 1, UnidadMedida.UNIDADES, null, EstadoUso.USADO, subCategoria);
    DonacionesRepository donacionesRepository = new DonacionesRepository();
    CargaDonacion cargaDonacion = new CargaDonacion(LocalDate.now(), donacionesRepository);

    cargaDonacion.cargarDonacion(crearDonanteDummy(), List.of(silla));

    assertEquals(1, donacionesRepository.getDonaciones().size());
  }

  @Test
  void matchmakingGuardaCoincidenciasCuandoAmbosAlgoritmosCoinciden() {
    LocalDate fecha = LocalDate.of(2026, 7, 6);
    SubCategoria alimentos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Donante donante = crearDonanteDummy();
    Donacion donacion =
        new Donacion(donante, List.of(crearBien("Fideos", 10, alimentos, fecha.plusMonths(6))));
    DonacionesRepository donacionesRepository = new DonacionesRepository();
    donacionesRepository.addDonacion(donacion);

    EntidadBeneficiaria hogarA =
        crearEntidadConNecesidad("Hogar A", alimentos, 5, fecha.plusMonths(4));
    EntidadBeneficiaria hogarB =
        crearEntidadConNecesidad("Hogar B", alimentos, 5, fecha.plusMonths(4));
    hogarB.registrarRecepcion(
        new Donacion(donante, List.of(crearBien("Vieja", 1, alimentos, fecha.plusMonths(1)))),
        fecha.minusDays(10));

    BeneficiariosRepository beneficiariosRepository = new BeneficiariosRepository();
    beneficiariosRepository.addBeneficiario(hogarA);
    beneficiariosRepository.addBeneficiario(hogarB);

    PropuestaRepository propuestaRepository = new PropuestaRepositoryImpl();
    ServicioMatchmakingDonaciones servicio =
        new ServicioMatchmakingDonaciones(
            donacionesRepository,
            beneficiariosRepository,
            propuestaRepository,
            List.of(
                new AlgoritmoCompatibilidadSemantica(),
                new AlgoritmoPrioridadSubAtendidos(mock(DonacionesRepository.class))));

    servicio.ejecutarMatchmakingDiario(fecha);

    PropuestaDeAsignacion propuesta =
        propuestaRepository.findPendingByDonacion(donacion).orElse(null);
    assertNotNull(propuesta, "Matchmaking debe crear una PropuestaDeAsignacion");
    assertTrue(propuesta.isPendiente(), "Propuesta debe estar pendiente");
    assertEquals(
        2,
        propuesta.getRankings().size(),
        "Propuesta debe contener los rankings de ambos algoritmos");
  }

  @Test
  void matchmakingGuardaRankingsSeparadosCuandoNoHayCoincidencias() {
    LocalDate fecha = LocalDate.of(2026, 7, 6);
    SubCategoria alimentos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    SubCategoria ropa = new SubCategoria("Abrigos", Categoria.VESTIMENTA);
    Donante donante = crearDonanteDummy();
    Donacion donacion =
        new Donacion(donante, List.of(crearBien("Fideos", 10, alimentos, fecha.plusMonths(6))));
    DonacionesRepository donacionesRepository = new DonacionesRepository();
    donacionesRepository.addDonacion(donacion);

    EntidadBeneficiaria compatible =
        crearEntidadConNecesidad("Compatible", alimentos, 5, fecha.plusMonths(4));
    EntidadBeneficiaria noCompatible =
        crearEntidadConNecesidadNoPerecedera("Sin match semántico", ropa, 2);

    BeneficiariosRepository beneficiariosRepository = new BeneficiariosRepository();
    beneficiariosRepository.addBeneficiario(compatible);
    beneficiariosRepository.addBeneficiario(noCompatible);

    PropuestaRepository propuestaRepository = new PropuestaRepositoryImpl();
    ServicioMatchmakingDonaciones servicio =
        new ServicioMatchmakingDonaciones(
            donacionesRepository,
            beneficiariosRepository,
            propuestaRepository,
            List.of(
                new AlgoritmoCompatibilidadSemantica(),
                new AlgoritmoMatchmaking() {
                  @Override
                  public String getNombre() {
                    return "algoritmo-manual";
                  }

                  @Override
                  public RankingMatchmaking rankear(
                      Donacion d,
                      java.util.Collection<EntidadBeneficiaria> entidades,
                      LocalDate fechaReferencia) {
                    return new RankingMatchmaking(
                        getNombre(), List.of(new MatchCandidato(noCompatible, 99)));
                  }
                }));

    servicio.ejecutarMatchmakingDiario(fecha);

    PropuestaDeAsignacion propuesta =
        propuestaRepository.findPendingByDonacion(donacion).orElse(null);
    assertNotNull(propuesta, "Matchmaking debe crear una PropuestaDeAsignacion");
    assertEquals(
        2,
        propuesta.getRankings().size(),
        "Propuesta debe contener los rankings de ambos algoritmos");
  }

  @Test
  void matchmaking_SaltaDonacionConPropuestaPendiente() {
    LocalDate fecha = LocalDate.of(2026, 7, 6);
    SubCategoria alimentos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Donante donante = crearDonanteDummy();
    Donacion donacion =
        new Donacion(donante, List.of(crearBien("Fideos", 10, alimentos, fecha.plusMonths(6))));
    DonacionesRepository donacionesRepository = new DonacionesRepository();
    donacionesRepository.addDonacion(donacion);
    BeneficiariosRepository beneficiariosRepository = new BeneficiariosRepository();
    beneficiariosRepository.addBeneficiario(
        crearEntidadConNecesidad("Entidad", alimentos, 5, fecha.plusMonths(4)));

    PropuestaRepository propuestaRepository = new PropuestaRepositoryImpl();
    ServicioMatchmakingDonaciones servicio =
        new ServicioMatchmakingDonaciones(
            donacionesRepository,
            beneficiariosRepository,
            propuestaRepository,
            List.of(
                new AlgoritmoCompatibilidadSemantica(),
                new AlgoritmoPrioridadSubAtendidos(mock(DonacionesRepository.class))));

    // First execution creates Propuesta
    servicio.ejecutarMatchmakingDiario(fecha);
    PropuestaDeAsignacion primera =
        propuestaRepository.findPendingByDonacion(donacion).orElse(null);
    assertNotNull(primera, "Primer matchmaking debe crear Propuesta");

    // Second execution should NOT replace
    servicio.ejecutarMatchmakingDiario(fecha);
    PropuestaDeAsignacion segunda =
        propuestaRepository.findPendingByDonacion(donacion).orElse(null);
    assertNotNull(segunda, "Propuesta debe seguir existiendo");
    assertSame(primera, segunda, "Segundo matchmaking no debe reemplazar la Propuesta pendiente");
  }

  @Test
  void confirmarAsignacionAdministrativa_DelegaEnPropuesta() {
    LocalDate fecha = LocalDate.of(2026, 7, 6);
    SubCategoria alimentos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Donante donante = crearDonanteDummy();
    Donacion donacion =
        new Donacion(donante, List.of(crearBien("Fideos", 10, alimentos, fecha.plusMonths(6))));
    DonacionesRepository donacionesRepository = new DonacionesRepository();
    donacionesRepository.addDonacion(donacion);
    BeneficiariosRepository beneficiariosRepository = new BeneficiariosRepository();
    EntidadBeneficiaria entidad =
        crearEntidadConNecesidad("Hogar", alimentos, 5, fecha.plusMonths(4));

    PropuestaRepository propuestaRepository = new PropuestaRepositoryImpl();
    ServicioMatchmakingDonaciones servicio =
        new ServicioMatchmakingDonaciones(
            donacionesRepository,
            beneficiariosRepository,
            propuestaRepository,
            List.of(
                new AlgoritmoCompatibilidadSemantica(),
                new AlgoritmoPrioridadSubAtendidos(mock(DonacionesRepository.class))));

    // Run matchmaking to create Propuesta first
    servicio.ejecutarMatchmakingDiario(fecha);

    // Now confirm
    servicio.confirmarAsignacionAdministrativa(donacion, entidad, fecha);

    // Propuesta should be removed from repo
    assertTrue(
        propuestaRepository.findPendingByDonacion(donacion).isEmpty(),
        "Propuesta debe ser removida del repositorio tras confirmar");
    assertEquals(
        entidad, donacion.getEntidadBeneficiaria(), "Donacion debe tener la entidad beneficiaria");
    assertInstanceOf(
        AsignacionRealizada.class,
        donacion.getEstado(),
        "Donacion debe estar en AsignacionRealizada");
    assertEquals(
        0,
        entidad.getRecepciones().size(),
        "confirmarAsignacionAdministrativa NO debe llamar registrarRecepcion");
  }

  @Test
  void registrarRecepcionDirectamenteRegistraYSatisfaceNecesidad() {
    LocalDate fecha = LocalDate.of(2026, 7, 6);
    SubCategoria alimentos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Donante donante = crearDonanteDummy();
    Donacion donacion =
        new Donacion(donante, List.of(crearBien("Fideos", 10, alimentos, fecha.plusMonths(6))));
    EntidadBeneficiaria entidad =
        crearEntidadConNecesidad("Hogar", alimentos, 5, fecha.plusMonths(4));
    Necesidad necesidad = entidad.getNecesidadesExtraordinarias().get(0);

    entidad.registrarRecepcion(donacion, fecha);

    assertEquals(1, entidad.getRecepciones().size());
    assertTrue(necesidad.estaSatisfecha());
  }

  @Test
  void prioridadSubAtendidosUsaRecepcionesDelUltimoTrimestre() {
    LocalDate fecha = LocalDate.of(2026, 7, 6);
    SubCategoria alimentos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Donante donante = crearDonanteDummy();
    EntidadBeneficiaria entidadMenosAtendida =
        crearEntidadConNecesidad("Menos Atendida", alimentos, 5, fecha.plusMonths(4));
    EntidadBeneficiaria entidadMasAtendida =
        crearEntidadConNecesidad("Mas Atendida", alimentos, 5, fecha.plusMonths(4));
    Donacion donacion =
        new Donacion(donante, List.of(crearBien("Fideos", 10, alimentos, fecha.plusMonths(6))));

    entidadMasAtendida.registrarRecepcion(
        new Donacion(donante, List.of(crearBien("Vieja", 1, alimentos, fecha.plusMonths(1)))),
        fecha.minusDays(5));
    entidadMasAtendida.registrarRecepcion(
        new Donacion(donante, List.of(crearBien("Vieja 2", 1, alimentos, fecha.plusMonths(1)))),
        fecha.minusDays(15));
    entidadMasAtendida.registrarRecepcion(
        new Donacion(
            donante, List.of(crearBien("Fuera trimestre", 1, alimentos, fecha.plusMonths(1)))),
        fecha.minusMonths(4));

    DonacionesRepository repo = mock(DonacionesRepository.class);
    when(repo.contarDonacionesAsignadas(eq(entidadMasAtendida), any())).thenReturn(2L);
    when(repo.contarDonacionesAsignadas(eq(entidadMenosAtendida), any())).thenReturn(0L);

    var ranking =
        new AlgoritmoPrioridadSubAtendidos(repo)
            .rankear(donacion, List.of(entidadMasAtendida, entidadMenosAtendida), fecha);

    assertEquals(
        "Menos Atendida", ranking.getCandidatos().get(0).getEntidadBeneficiaria().getRazonSocial());
  }

  @Test
  void schedulerCalculaDemoraHastaLaProximaMedianoche() {
    Clock clock = Clock.fixed(Instant.parse("2026-07-06T21:30:00Z"), ZoneId.of("UTC"));

    long demora = MatchmakingScheduler.milisegundosHastaProximaMedianoche(clock);

    assertEquals(9_000_000L, demora);
  }

  private static Bien crearBien(
      String descripcion, double cantidad, SubCategoria subCategoria, LocalDate fechaVencimiento) {
    return new Bien(
        descripcion,
        "",
        cantidad,
        UnidadMedida.KILOGRAMOS,
        fechaVencimiento,
        EstadoUso.NO_APLICA,
        subCategoria);
  }

  private static EntidadBeneficiaria crearEntidadConNecesidad(
      String razonSocial, SubCategoria subCategoria, double cantidad, LocalDate fechaVencimiento) {
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(razonSocial, "Direccion", "Telefono");
    entidad.agregarNecesidadExtraordinaria(
        new Necesidad(subCategoria, cantidad, UnidadMedida.KILOGRAMOS, "Necesidad"));
    return entidad;
  }

  private static EntidadBeneficiaria crearEntidadConNecesidadNoPerecedera(
      String razonSocial, SubCategoria subCategoria, double cantidad) {
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(razonSocial, "Direccion", "Telefono");
    entidad.agregarNecesidadExtraordinaria(
        new Necesidad(subCategoria, cantidad, UnidadMedida.UNIDADES, "Necesidad"));
    return entidad;
  }
}
