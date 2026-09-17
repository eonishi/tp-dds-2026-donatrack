package ar.edu.utn.donatrack.persistencia;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.beneficiarios.NecesidadRecurrente;
import ar.edu.utn.donatrack.beneficiarios.TipoPeriodo;
import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.bienes.Categoria;
import ar.edu.utn.donatrack.bienes.EstadoUso;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.bienes.UnidadMedida;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.HistorialEstado;
import ar.edu.utn.donatrack.donaciones.NombreEstado;
import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.Genero;
import ar.edu.utn.donatrack.donantes.MedioDeContacto;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.PersonajeJuridica;
import ar.edu.utn.donatrack.donantes.Representante;
import ar.edu.utn.donatrack.donantes.Sms;
import ar.edu.utn.donatrack.donantes.TipoDocumento;
import ar.edu.utn.donatrack.donantes.TipoOrganizacion;
import ar.edu.utn.donatrack.donantes.Whatsapp;
import ar.edu.utn.donatrack.estados.AsignacionRealizada;
import ar.edu.utn.donatrack.estados.EnDeposito;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/**
 * Demuestra que el modelo de dominio de los cuatro agregados respaldados por JPA realmente puede
 * persistirse y releerse desde la base.
 *
 * <p>El entity manager se limpia antes de volver a consultar, de modo que cada aserción corre
 * contra una instancia nueva cargada desde HSQLDB y no contra la caché de primer nivel del entity
 * manager.
 */
class PersistenciaModeloTest implements SimplePersistenceTest {

  private static final String DONANTE_EMAIL = "juan@test.com";

  @Test
  void modeloCompleto_SobreviveAlViajeDeIdaYVueltaPorLaBase() {
    SubCategoria fideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);

    Donante donante = crearDonanteHumano();
    Donante organizacion = crearOrganizacion();

    EntidadBeneficiaria entidad =
        new EntidadBeneficiaria("Hogar Test", "Av. Siempreviva 742", "1145557777");
    NecesidadRecurrente necesidad =
        new NecesidadRecurrente(fideos, 20.0, UnidadMedida.KILOGRAMOS, "Fideos del mes");
    necesidad.setPeriodo(TipoPeriodo.MENSUAL);
    entidad.agregarNecesidadRecurrente(necesidad);

    Donacion enDeposito = new Donacion(donante, List.of(crearBien(fideos, 5.0, "Fideos comunes")));
    Donacion asignada = new Donacion(donante, List.of(crearBien(fideos, 3.0, "Fideos integrales")));
    // La donación transiciona antes de ser persistida, como hace el flujo actual de la aplicación.
    asignada.asignar();
    // El registro asienta la transición inicial; no debe agregar una segunda entrada a una donación
    // que ya transitó antes de ser registrada.
    enDeposito.registrarEnDeposito(LocalDate.now());
    asignada.registrarEnDeposito(LocalDate.now());

    persist(fideos);
    persist(organizacion);
    persist(entidad);
    persist(donante);
    persist(enDeposito);
    persist(asignada);
    entityManager().flush();
    entityManager().clear();

    Donante donanteRecargado = recargarDonante("12345678");
    assertEquals(
        DONANTE_EMAIL,
        donanteRecargado.extraerEmail().orElseThrow(),
        "El correo electrónico debe recuperarse desde la base");

    List<MedioDeContacto> medios = donanteRecargado.getMediosDeContacto();
    assertEquals(3, medios.size(), "El donante debe conservar sus tres medios de contacto");
    assertTrue(esUnMedio(medios, CorreoElectronico.class::isInstance), "Falta el correo");
    assertTrue(esUnMedio(medios, Sms.class::isInstance), "Falta el SMS");
    assertTrue(esUnMedio(medios, Whatsapp.class::isInstance), "Falta el WhatsApp");

    PersonajeJuridica organizacionRecargada = (PersonajeJuridica) recargarDonante("30111222333");
    assertEquals(
        "ong@test.com",
        organizacionRecargada.extraerEmail().orElseThrow(),
        "La organización debe conservar su propio correo");
    assertEquals(1, organizacionRecargada.getRepresentantes().size());
    assertEquals("Ana", organizacionRecargada.getRepresentantes().get(0).getNombre());

    EntidadBeneficiaria entidadRecargada = recargarEntidad();
    assertEquals(1, entidadRecargada.getNecesidadesRecurrentes().size());
    NecesidadRecurrente necesidadRecargada = entidadRecargada.getNecesidadesRecurrentes().get(0);
    assertEquals("Fideos Secos", necesidadRecargada.getSubcategoria().getNombre());
    assertEquals(20.0, necesidadRecargada.getCantidadObjetivo(), 0.001);
    assertEquals(
        TipoPeriodo.MENSUAL, necesidadRecargada.getPeriodo(), "El período debe persistirse");

    List<Donacion> donaciones = recargarDonaciones();
    Donacion depositoRecargada = buscarPorBien(donaciones, "Fideos comunes");
    Donacion asignadaRecargada = buscarPorBien(donaciones, "Fideos integrales");
    assertEquals(
        "Fideos Secos",
        depositoRecargada.getBienes().get(0).getSubcategoria().getNombre(),
        "El bien debe seguir referenciando la misma subcategoría");

    assertEquals(
        NombreEstado.EN_DEPOSITO,
        depositoRecargada.getNombreEstado(),
        "Una donación en depósito debe recargarse en depósito");
    assertInstanceOf(EnDeposito.class, depositoRecargada.getEstado());
    List<HistorialEstado> historialDeposito = depositoRecargada.getHistorialEstado();
    assertEquals(1, historialDeposito.size(), "El registro inicial debe dejar una transición");
    assertEquals(NombreEstado.EN_DEPOSITO, historialDeposito.get(0).getEstadoAlcanzado());
    assertEquals(
        NombreEstado.ASIGNACION_REALIZADA,
        asignadaRecargada.getNombreEstado(),
        "El estado asignado debe derivarse del historial persistido");
    assertInstanceOf(AsignacionRealizada.class, asignadaRecargada.getEstado());
    List<HistorialEstado> historialAsignada = asignadaRecargada.getHistorialEstado();
    assertEquals(1, historialAsignada.size(), "La asignación debe dejar una única transición");
    assertEquals(NombreEstado.ASIGNACION_REALIZADA, historialAsignada.get(0).getEstadoAlcanzado());
  }

  @Test
  void historialConVariasTransiciones_RecargaLaUltimaPorId() {
    SubCategoria fideos = new SubCategoria("Arroz", Categoria.ALIMENTO);
    Donante donante = crearDonanteHumano();
    Donacion donacion = new Donacion(donante, List.of(crearBien(fideos, 2.0, "Arroz")));
    // Dos transiciones que comparten la misma fecha: la última es la de mayor id.
    donacion.asignar();
    donacion.planificarRuta();

    persist(fideos);
    persist(donante);
    persist(donacion);
    entityManager().flush();
    entityManager().clear();

    Donacion recargada = buscarPorBien(recargarDonaciones(), "Arroz");
    assertEquals(2, recargada.getHistorialEstado().size(), "Ambas transiciones deben persistirse");
    assertEquals(
        NombreEstado.LISTA_PARA_ENTREGAR,
        recargada.getNombreEstado(),
        "Debe derivarse la última transición, no la primera");
  }

  private static boolean esUnMedio(List<MedioDeContacto> medios, Predicate<MedioDeContacto> tipo) {
    return medios.stream().anyMatch(tipo);
  }

  private static Donante crearDonanteHumano() {
    PersonaHumana donante =
        new PersonaHumana(
            new CorreoElectronico(DONANTE_EMAIL),
            "Juan",
            "Perez",
            30,
            "12345678",
            TipoDocumento.DNI,
            Genero.MASCULINO,
            "Calle Falsa 123");
    donante.addMedioDeContacto(new Sms("1145558888"));
    donante.addMedioDeContacto(new Whatsapp("1145559999"));
    return donante;
  }

  private static Donante crearOrganizacion() {
    PersonajeJuridica organizacion =
        new PersonajeJuridica(
            new CorreoElectronico("ong@test.com"),
            "30111222333",
            TipoDocumento.CUIT,
            "Fundación Test",
            TipoOrganizacion.ONG,
            "Alimentos");
    organizacion.agregarRepresentante(new Representante("Ana", "Gomez"));
    return organizacion;
  }

  private static Bien crearBien(SubCategoria subcategoria, double cantidad, String descripcion) {
    return new Bien(
        descripcion,
        "",
        cantidad,
        UnidadMedida.KILOGRAMOS,
        LocalDate.now().plusMonths(6),
        EstadoUso.NO_APLICA,
        subcategoria);
  }

  private Donante recargarDonante(String numDocumento) {
    return createQuery("select d from Donante d where d.numDocumento = :doc", Donante.class)
        .setParameter("doc", numDocumento)
        .getSingleResult();
  }

  private EntidadBeneficiaria recargarEntidad() {
    return createQuery(
            "select e from EntidadBeneficiaria e where e.razonSocial = :razon",
            EntidadBeneficiaria.class)
        .setParameter("razon", "Hogar Test")
        .getSingleResult();
  }

  private List<Donacion> recargarDonaciones() {
    return createQuery("select d from Donacion d", Donacion.class).getResultList();
  }

  private static Donacion buscarPorBien(List<Donacion> donaciones, String descripcion) {
    return donaciones.stream()
        .filter(d -> d.getBienes().stream().anyMatch(b -> descripcion.equals(b.getDescripcion())))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No se recargó la donación con " + descripcion));
  }
}
