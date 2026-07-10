package ar.edu.utn.donatrack;

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
import ar.edu.utn.donatrack.donaciones.MatchCandidato;
import ar.edu.utn.donatrack.donaciones.MatchmakingScheduler;
import ar.edu.utn.donatrack.donaciones.RankingMatchmaking;
import ar.edu.utn.donatrack.donaciones.ResultadoMatchmaking;
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

    ServicioMatchmakingDonaciones servicio =
        new ServicioMatchmakingDonaciones(
            donacionesRepository,
            beneficiariosRepository,
            List.of(new AlgoritmoCompatibilidadSemantica(), new AlgoritmoPrioridadSubAtendidos()));

    servicio.ejecutarMatchmakingDiario(fecha);

    ResultadoMatchmaking resultado = donacion.getResultadoMatchmakingVigente();
    assertNotNull(resultado);
    assertTrue(resultado.tieneCoincidencias());
    assertEquals(2, resultado.getCoincidencias().size());
    assertEquals(2, resultado.getRankings().size());
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

    ServicioMatchmakingDonaciones servicio =
        new ServicioMatchmakingDonaciones(
            donacionesRepository,
            beneficiariosRepository,
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

    ResultadoMatchmaking resultado = donacion.getResultadoMatchmakingVigente();
    assertNotNull(resultado);
    assertFalse(resultado.tieneCoincidencias());
    assertEquals(2, resultado.getRankings().size());
  }

  @Test
  void confirmarAsignacionAdministrativa_NoRegistraRecepcionNiSatisfaceNecesidades() {
    LocalDate fecha = LocalDate.of(2026, 7, 6);
    SubCategoria alimentos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    Donante donante = crearDonanteDummy();
    Donacion donacion =
        new Donacion(donante, List.of(crearBien("Fideos", 10, alimentos, fecha.plusMonths(6))));
    DonacionesRepository donacionesRepository = new DonacionesRepository();
    BeneficiariosRepository beneficiariosRepository = new BeneficiariosRepository();
    EntidadBeneficiaria entidad =
        crearEntidadConNecesidad("Hogar", alimentos, 5, fecha.plusMonths(4));
    Necesidad necesidad = entidad.getNecesidadesExtraordinarias().get(0);

    ServicioMatchmakingDonaciones servicio =
        new ServicioMatchmakingDonaciones(
            donacionesRepository,
            beneficiariosRepository,
            List.of(new AlgoritmoCompatibilidadSemantica(), new AlgoritmoPrioridadSubAtendidos()));

    servicio.confirmarAsignacionAdministrativa(donacion, entidad, fecha);

    assertEquals(entidad, donacion.getEntidadBeneficiaria());
    assertInstanceOf(AsignacionRealizada.class, donacion.getEstado());
    assertEquals(0, entidad.getRecepciones().size());
    assertFalse(necesidad.estaSatisfecha());
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

    var ranking =
        new AlgoritmoPrioridadSubAtendidos()
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
