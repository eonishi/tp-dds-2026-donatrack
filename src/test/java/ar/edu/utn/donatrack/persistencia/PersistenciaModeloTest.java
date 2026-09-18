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
import ar.edu.utn.donatrack.donaciones.EstadoDonacion;
import ar.edu.utn.donatrack.donaciones.HistorialEstado;
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
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
  private static final String MIGRACION =
      "db/migrations/2026-09-19-estado-de-donacion-a-nombre.sql";

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
        EstadoDonacion.EN_DEPOSITO,
        depositoRecargada.getEstado(),
        "Una donación en depósito debe recargarse en depósito");
    assertEquals(EstadoDonacion.EN_DEPOSITO, depositoRecargada.getEstado());
    List<HistorialEstado> historialDeposito = depositoRecargada.getHistorialEstado();
    assertEquals(1, historialDeposito.size(), "El registro inicial debe dejar una transición");
    assertEquals(EstadoDonacion.EN_DEPOSITO, historialDeposito.get(0).getEstadoAlcanzado());
    assertEquals(
        EstadoDonacion.ASIGNACION_REALIZADA,
        asignadaRecargada.getEstado(),
        "El estado asignado debe derivarse del historial persistido");
    assertEquals(EstadoDonacion.ASIGNACION_REALIZADA, asignadaRecargada.getEstado());
    List<HistorialEstado> historialAsignada = asignadaRecargada.getHistorialEstado();
    assertEquals(1, historialAsignada.size(), "La asignación debe dejar una única transición");
    assertEquals(
        EstadoDonacion.ASIGNACION_REALIZADA, historialAsignada.get(0).getEstadoAlcanzado());
  }

  @Test
  void historialConVariasTransiciones_ConservaElEstadoAlcanzado() {
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
        EstadoDonacion.LISTA_PARA_ENTREGAR,
        recargada.getEstado(),
        "El estado actual debe ser la última transición, no la primera");
  }

  @Test
  void lasColumnasDeEstadoGuardanElNombreYNoElIndice() {
    SubCategoria arroz = new SubCategoria("Quinoa", Categoria.ALIMENTO);
    Donante donante = crearDonanteHumano();
    Donacion donacion = new Donacion(donante, List.of(crearBien(arroz, 1.0, "Quinoa")));
    donacion.asignar();

    persist(arroz);
    persist(donante);
    persist(donacion);
    entityManager().flush();

    // Consultas nativas: esquivan el mapeo de Hibernate y leen el valor crudo de cada columna.
    Object crudoHistorial =
        entityManager()
            .createNativeQuery("SELECT estadoAlcanzado FROM HistorialEstado")
            .getSingleResult();
    Object crudoDonacion =
        entityManager().createNativeQuery("SELECT estado FROM Donacion").getSingleResult();

    assertEquals(
        "ASIGNACION_REALIZADA",
        crudoHistorial,
        "La columna del historial debe guardar el nombre del enum, no su índice");
    assertEquals(
        "ASIGNACION_REALIZADA",
        crudoDonacion,
        "La columna de la donación debe guardar el nombre del enum, no su índice");
  }

  @Test
  void elEstadoActualSobreviveSinElHistorial() {
    SubCategoria fideos = new SubCategoria("Lentejas", Categoria.ALIMENTO);
    Donante donante = crearDonanteHumano();
    Donacion donacion = new Donacion(donante, List.of(crearBien(fideos, 4.0, "Lentejas")));
    donacion.asignar();

    persist(fideos);
    persist(donante);
    persist(donacion);
    entityManager().flush();

    entityManager().createQuery("DELETE FROM HistorialEstado").executeUpdate();
    entityManager().flush();
    entityManager().clear();

    Donacion recargada = buscarPorBien(recargarDonaciones(), "Lentejas");
    assertTrue(
        recargada.getHistorialEstado().isEmpty(),
        "El historial debe quedar vacío tras el borrado masivo");
    assertEquals(
        EstadoDonacion.ASIGNACION_REALIZADA,
        recargada.getEstado(),
        "El estado actual debe estar materializado: sobrevive sin historial del cual derivarlo");
  }

  @Test
  void lasColumnasDeEstadoSonObligatoriasYDeTexto() {
    // El estado decide qué donaciones devuelve un filtro: una fila sin estado quedaría invisible en
    // silencio, y columnas numéricas volverían a admitir el índice como si fuera un estado.
    assertColumnaDeEstado("Donacion", "estado");
    assertColumnaDeEstado("HistorialEstado", "estadoAlcanzado");
  }

  private void assertColumnaDeEstado(String tabla, String columna) {
    Object[] fila =
        (Object[])
            entityManager()
                .createNativeQuery(
                    "SELECT is_nullable, data_type FROM information_schema.columns "
                        + "WHERE upper(table_name) = upper(:tabla) "
                        + "AND upper(column_name) = upper(:columna)")
                .setParameter("tabla", tabla)
                .setParameter("columna", columna)
                .getSingleResult();
    String donde = tabla + "." + columna;
    assertEquals("NO", fila[0], "La columna " + donde + " no debe admitir null");
    assertTrue(
        String.valueOf(fila[1]).contains("CHAR"),
        "La columna " + donde + " debe ser de texto y no numérica, pero es " + fila[1]);
  }

  @Test
  void unaFilaLegadaConOrdinalFallaEnVozAlta() {
    SubCategoria trigo = new SubCategoria("Trigo", Categoria.ALIMENTO);
    Donante donante = crearDonanteHumano();
    Donacion donacion = new Donacion(donante, List.of(crearBien(trigo, 1.0, "Trigo")));

    persist(trigo);
    persist(donante);
    persist(donacion);
    entityManager().flush();

    // El esquema anterior guardaba el estado como índice. Esa fila no se corresponde con ningún
    // nombre de constante, así que debe fallar al hidratar en vez de reinterpretarse en silencio.
    Long donacionId = createQuery("select d.id from Donacion d", Long.class).getSingleResult();
    entityManager()
        .createNativeQuery(
            "INSERT INTO HistorialEstado (fecha, estadoAlcanzado, justificacion, donacion_id) "
                + "VALUES (CURRENT_DATE, '1', 'fila legada', :id)")
        .setParameter("id", donacionId)
        .executeUpdate();
    entityManager().clear();

    assertThrows(
        RuntimeException.class,
        () -> createQuery("select h from HistorialEstado h", HistorialEstado.class).getResultList(),
        "Un estado legado guardado como ordinal debe fallar, no reinterpretarse");
  }

  @Test
  void laMigracionConvierteElOrdinalGuardadoEnNombre() throws IOException {
    SubCategoria maiz = new SubCategoria("Maíz", Categoria.ALIMENTO);
    Donante donante = crearDonanteHumano();
    Donacion donacion = new Donacion(donante, List.of(crearBien(maiz, 1.0, "Maíz")));
    persist(maiz);
    persist(donante);
    persist(donacion);
    entityManager().flush();

    // La misma fila legada que el test anterior: el esquema viejo guardaba el índice.
    Long donacionId = createQuery("select d.id from Donacion d", Long.class).getSingleResult();
    entityManager()
        .createNativeQuery(
            "INSERT INTO HistorialEstado (fecha, estadoAlcanzado, justificacion, donacion_id) "
                + "VALUES (CURRENT_DATE, '1', 'fila legada', :id)")
        .setParameter("id", donacionId)
        .executeUpdate();
    entityManager().clear();

    // La migración se ejecuta tal como se entrega: las sentencias salen del archivo.
    List<String> sentencias =
        List.of(Files.readString(Path.of(MIGRACION)).replaceAll("(?m)^--.*$", "").split(";"))
            .stream()
            .map(String::trim)
            .filter(sentencia -> sentencia.startsWith("UPDATE"))
            .toList();
    assertFalse(sentencias.isEmpty(), "La migración debe traer las sentencias de conversión");
    for (String sentencia : sentencias) {
      entityManager().createNativeQuery(sentencia).executeUpdate();
    }
    entityManager().flush();
    entityManager().clear();

    HistorialEstado migrada =
        createQuery("select h from HistorialEstado h", HistorialEstado.class).getSingleResult();
    assertEquals(
        EstadoDonacion.ASIGNACION_REALIZADA,
        migrada.getEstadoAlcanzado(),
        "La migración debe convertir el ordinal guardado en el nombre de la constante");
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
