package ar.edu.utn.donatrack;

import static ar.edu.utn.donatrack.testsupport.FabricaDePrueba.nuevaDonacion;
import static ar.edu.utn.donatrack.testsupport.FabricaDePrueba.nuevaEntidad;
import static org.junit.jupiter.api.Assertions.*;

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
import ar.edu.utn.donatrack.donaciones.EstadoDonacion;
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
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

public class MatchmakingDonacionesTest implements SimplePersistenceTest {

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
    Donante donante = crearDonanteDummy();
    // CargaDonacion persiste cada donación segmentada, y una donación solo cascadea a sus bienes:
    // el donante y la subcategoría del bien ya deben estar almacenados.
    persist(subCategoria);
    persist(donante);
    DonacionesRepository donacionesRepository = new DonacionesRepository();
    CargaDonacion cargaDonacion = new CargaDonacion(LocalDate.now(), donacionesRepository);

    cargaDonacion.cargarDonacion(donante, List.of(silla));

    assertEquals(1, donacionesRepository.getDonaciones().size());
  }

  @Test
  void matchmakingGuardaCoincidenciasCuandoAmbosAlgoritmosCoinciden() {
    LocalDate fecha = LocalDate.of(2026, 7, 6);
    SubCategoria alimentos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Donante donante = crearDonanteDummy();
    Donacion donacion =
        nuevaDonacion(donante, List.of(crearBien("Fideos", 10, alimentos, fecha.plusMonths(6))));
    // El repositorio persiste la donación que recibe, y una donación solo cascadea a sus bienes:
    // el donante, la subcategoría del bien y la entidad beneficiaria ya deben estar almacenados.
    // El repositorio de beneficiarios ya es JPA, pero la donación no persiste su entidad: las
    // entidades se almacenan acá porque la donación entregada referencia a hogarB.
    persist(alimentos);
    persist(donante);
    DonacionesRepository donacionesRepository = new DonacionesRepository();
    donacionesRepository.addDonacion(donacion);

    EntidadBeneficiaria hogarA =
        crearEntidadConNecesidad("Hogar A", alimentos, 5, fecha.plusMonths(4));
    EntidadBeneficiaria hogarB =
        crearEntidadConNecesidad("Hogar B", alimentos, 5, fecha.plusMonths(4));
    persist(hogarA);
    persist(hogarB);
    donacionesRepository.addDonacion(crearDonacionEntregada(donante, hogarB, alimentos));

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
                new AlgoritmoPrioridadSubAtendidos(donacionesRepository)));

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
        nuevaDonacion(donante, List.of(crearBien("Fideos", 10, alimentos, fecha.plusMonths(6))));
    // El repositorio persiste la donación que recibe: su donante y las subcategorías de su bien y
    // de la necesidad ya deben estar almacenados.
    persist(alimentos);
    persist(ropa);
    persist(donante);
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
        nuevaDonacion(donante, List.of(crearBien("Fideos", 10, alimentos, fecha.plusMonths(6))));
    // El repositorio persiste la donación que recibe: su donante y la subcategoría de su bien ya
    // deben estar almacenados.
    persist(alimentos);
    persist(donante);
    DonacionesRepository donacionesRepository = new DonacionesRepository();
    donacionesRepository.addDonacion(donacion);
    BeneficiariosRepository beneficiariosRepository = new BeneficiariosRepository();
    EntidadBeneficiaria entidad =
        crearEntidadConNecesidad("Entidad", alimentos, 5, fecha.plusMonths(4));
    // El ranking real consulta las entregas por entidad: la entidad debe estar almacenada.
    persist(entidad);
    beneficiariosRepository.addBeneficiario(entidad);

    PropuestaRepository propuestaRepository = new PropuestaRepositoryImpl();
    ServicioMatchmakingDonaciones servicio =
        new ServicioMatchmakingDonaciones(
            donacionesRepository,
            beneficiariosRepository,
            propuestaRepository,
            List.of(
                new AlgoritmoCompatibilidadSemantica(),
                new AlgoritmoPrioridadSubAtendidos(donacionesRepository)));

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
        nuevaDonacion(donante, List.of(crearBien("Fideos", 10, alimentos, fecha.plusMonths(6))));
    // El repositorio persiste la donación que recibe: su donante y la subcategoría de su bien ya
    // deben estar almacenados.
    persist(alimentos);
    persist(donante);
    DonacionesRepository donacionesRepository = new DonacionesRepository();
    donacionesRepository.addDonacion(donacion);
    BeneficiariosRepository beneficiariosRepository = new BeneficiariosRepository();
    EntidadBeneficiaria entidad =
        crearEntidadConNecesidad("Hogar", alimentos, 5, fecha.plusMonths(4));
    persist(entidad);
    beneficiariosRepository.addBeneficiario(entidad);

    PropuestaRepository propuestaRepository = new PropuestaRepositoryImpl();
    ServicioMatchmakingDonaciones servicio =
        new ServicioMatchmakingDonaciones(
            donacionesRepository,
            beneficiariosRepository,
            propuestaRepository,
            List.of(
                new AlgoritmoCompatibilidadSemantica(),
                new AlgoritmoPrioridadSubAtendidos(donacionesRepository)));

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
    assertEquals(
        EstadoDonacion.ASIGNACION_REALIZADA,
        donacion.getEstado(),
        "Donacion debe estar en AsignacionRealizada");
    assertEquals(
        0.0,
        entidad.getNecesidadesExtraordinarias().get(0).getCantidadRecibida(),
        0.001,
        "confirmarAsignacionAdministrativa NO debe aplicar la donación a las necesidades");
  }

  @Test
  void aplicarDonacionRecibida_SatisfaceNecesidad() {
    LocalDate fecha = LocalDate.of(2026, 7, 6);
    SubCategoria alimentos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Donante donante = crearDonanteDummy();
    Donacion donacion =
        nuevaDonacion(donante, List.of(crearBien("Fideos", 10, alimentos, fecha.plusMonths(6))));
    EntidadBeneficiaria entidad =
        crearEntidadConNecesidad("Hogar", alimentos, 5, fecha.plusMonths(4));
    Necesidad necesidad = entidad.getNecesidadesExtraordinarias().get(0);

    entidad.aplicarDonacionRecibida(donacion);

    assertTrue(necesidad.estaSatisfecha());
  }

  @Test
  void prioridadSubAtendidosUsaEntregasDelUltimoTrimestre() {
    SubCategoria alimentos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Donante donante = crearDonanteDummy();
    EntidadBeneficiaria entidad =
        crearEntidadConNecesidad("Sub Atendida", alimentos, 5, LocalDate.now().plusMonths(4));
    Donacion donacion =
        nuevaDonacion(
            donante, List.of(crearBien("Fideos", 10, alimentos, LocalDate.now().plusMonths(6))));
    DonacionesRepository donacionesRepository = new DonacionesRepository();
    // Las donaciones entregadas referencian a la entidad y la consulta de entregas la recibe como
    // parámetro: la entidad debe estar almacenada.
    persist(alimentos);
    persist(donante);
    persist(entidad);

    donacionesRepository.addDonacion(crearDonacionEntregada(donante, entidad, alimentos));
    donacionesRepository.addDonacion(crearDonacionEntregada(donante, entidad, alimentos));

    AlgoritmoPrioridadSubAtendidos algoritmo =
        new AlgoritmoPrioridadSubAtendidos(donacionesRepository);

    var rankingEnVentana = algoritmo.rankear(donacion, List.of(entidad), LocalDate.now());
    var rankingFueraDeVentana =
        algoritmo.rankear(donacion, List.of(entidad), LocalDate.now().plusMonths(4));

    assertEquals(
        98,
        rankingEnVentana.getCandidatos().get(0).getPuntaje(),
        "Las dos entregas dentro del trimestre deben descontar dos puntos del puntaje");
    assertEquals(
        100,
        rankingFueraDeVentana.getCandidatos().get(0).getPuntaje(),
        "Las entregas fuera del trimestre no deben contar: el puntaje debe quedar completo");
    assertTrue(
        rankingEnVentana.getCandidatos().get(0).getPuntaje()
            < rankingFueraDeVentana.getCandidatos().get(0).getPuntaje(),
        "Las entregas dentro del trimestre deben bajar el puntaje y las de afuera no deben contar");
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

  private static Donacion crearDonacionEntregada(
      Donante donante, EntidadBeneficiaria entidad, SubCategoria subCategoria) {
    Donacion donacion =
        nuevaDonacion(donante, List.of(crearBien("Entregada", 1, subCategoria, null)));
    donacion.confirmarEntidadBeneficiaria(entidad);
    donacion.cambiarEstado(EstadoDonacion.ENTREGADA, null);
    return donacion;
  }

  private static EntidadBeneficiaria crearEntidadConNecesidad(
      String razonSocial, SubCategoria subCategoria, double cantidad, LocalDate fechaVencimiento) {
    EntidadBeneficiaria entidad = nuevaEntidad(razonSocial);
    entidad.agregarNecesidadExtraordinaria(
        new Necesidad(subCategoria, cantidad, UnidadMedida.KILOGRAMOS, "Necesidad"));
    return entidad;
  }

  private static EntidadBeneficiaria crearEntidadConNecesidadNoPerecedera(
      String razonSocial, SubCategoria subCategoria, double cantidad) {
    EntidadBeneficiaria entidad = nuevaEntidad(razonSocial);
    entidad.agregarNecesidadExtraordinaria(
        new Necesidad(subCategoria, cantidad, UnidadMedida.UNIDADES, "Necesidad"));
    return entidad;
  }
}
