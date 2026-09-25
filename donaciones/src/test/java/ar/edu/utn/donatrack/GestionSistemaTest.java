package ar.edu.utn.donatrack;

import static ar.edu.utn.donatrack.testsupport.FabricaDePrueba.nuevaDonacion;
import static ar.edu.utn.donatrack.testsupport.FabricaDePrueba.nuevaEntidad;
import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.donatrack.beneficiarios.*;
import ar.edu.utn.donatrack.bienes.*;
import ar.edu.utn.donatrack.donaciones.*;
import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.Genero;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.TipoDocumento;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DonaTrackTest implements SimplePersistenceTest {

  private SubCategoria subcatSillas;
  private SubCategoria subcatFideos;
  private LocalDate hoy;
  private Donante donanteDummy;

  @BeforeEach
  void setUp() {
    subcatSillas = new SubCategoria("Sillas", Categoria.MOBILIARIO);
    subcatFideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    hoy = LocalDate.now();
    donanteDummy =
        new PersonaHumana(
            new CorreoElectronico("test@test.com"),
            "Juan",
            "Perez",
            30,
            "12345678",
            TipoDocumento.DNI,
            Genero.MASCULINO,
            "Calle Falsa 123");
    // CargaDonacion persiste cada donación que segmenta, y una donación solo cascadea a sus
    // bienes: el donante y las subcategorías de esos bienes ya deben estar almacenados.
    persist(donanteDummy);
    persist(subcatSillas);
    persist(subcatFideos);
  }

  // ==========================================
  // 1. TESTS DE SEGMENTACIÓN DE DONACIONES
  // ==========================================

  @Test
  void segmentarDonacion_AgrupaPorSubcategoria_EstadoUso_Y_Vencimiento() {
    // Arrange
    Bien sillaNueva =
        new Bien("Silla nueva", "", 2, UnidadMedida.UNIDADES, null, EstadoUso.NUEVO, subcatSillas);
    Bien sillaUsada =
        new Bien("Silla usada", "", 4, UnidadMedida.UNIDADES, null, EstadoUso.USADO, subcatSillas);
    Bien fideos2026 =
        new Bien(
            "Fideos que vencen 2026",
            "",
            10,
            UnidadMedida.KILOGRAMOS,
            hoy.plusYears(1),
            EstadoUso.NO_APLICA,
            subcatFideos);
    Bien fideos2027 =
        new Bien(
            "Fideos que vencen 2027",
            "",
            5,
            UnidadMedida.KILOGRAMOS,
            hoy.plusYears(2),
            EstadoUso.NO_APLICA,
            subcatFideos);
    Bien masFideos2026 =
        new Bien(
            "Más fideos mismo lote",
            "",
            5,
            UnidadMedida.KILOGRAMOS,
            hoy.plusYears(1),
            EstadoUso.NO_APLICA,
            subcatFideos);

    List<Bien> bienesDonados =
        Arrays.asList(sillaNueva, sillaUsada, fideos2026, fideos2027, masFideos2026);
    DonacionesRepository repo = new DonacionesRepository();
    CargaDonacion carga = new CargaDonacion(repo);

    // Act
    carga.cargarDonacion(donanteDummy, bienesDonados);

    // Assert
    // Debe haber 4 donaciones separadas:
    // 1. Sillas Nuevas
    // 2. Sillas Usadas
    // 3. Fideos 2026 (agrupando 'fideos2026' y 'masFideos2026')
    // 4. Fideos 2027
    assertEquals(
        4,
        repo.getDonaciones().size(),
        "La segmentación no agrupó correctamente las unidades mínimas.");

    // Verificamos que agrupó correctamente los fideos del mismo vencimiento
    boolean encontroGrupoFideos =
        repo.getDonaciones().stream()
            .anyMatch(d -> d.getBienes().size() == 2 && d.getSubcategoria().equals(subcatFideos));
    assertTrue(encontroGrupoFideos, "No agrupó los bienes idénticos en la misma donación.");
  }

  // ==========================================
  // 2. TESTS DE LA MÁQUINA DE ESTADOS
  // ==========================================

  @Test
  void estadosDonacion_TransicionesValidas_Y_Falla() {
    // Arrange
    Bien fideos =
        new Bien(
            "Fideos",
            "",
            10,
            UnidadMedida.KILOGRAMOS,
            hoy.plusYears(1),
            EstadoUso.NO_APLICA,
            subcatFideos);
    Donacion donacion = nuevaDonacion(donanteDummy, List.of(fideos));

    // Act & Assert
    // Estado inicial
    assertEquals(EstadoDonacion.EN_DEPOSITO, donacion.getEstado());

    // Transición: En Depósito -> Asignada
    donacion.asignar();
    assertEquals(EstadoDonacion.ASIGNACION_REALIZADA, donacion.getEstado());

    // Transición: Asignada -> Lista para entregar
    donacion.planificarRuta();
    assertEquals(EstadoDonacion.LISTA_PARA_ENTREGAR, donacion.getEstado());

    // Transición: Lista para entregar -> En traslado
    donacion.iniciarTraslado("http://test.mapa");
    assertEquals(EstadoDonacion.EN_TRASLADO, donacion.getEstado());

    // Transición: En traslado -> Entrega fallida (Único estado que requiere justificación según
    // enunciado)
    donacion.reportarFalla("Tocamos timbre pero nadie respondió");
    assertEquals(EstadoDonacion.ENTREGA_FALLIDA, donacion.getEstado());

    // Verificamos el historial para asegurar la trazabilidad
    List<HistorialEstado> historial = donacion.getHistorialEstado();
    assertEquals(4, historial.size(), "El historial no registró todas las transiciones.");
    assertEquals("Tocamos timbre pero nadie respondió", historial.get(3).getJustificacion());
  }

  @Test
  void estadosDonacion_TransicionInvalidaLanzaExcepcion() {
    // Arrange
    Bien fideos =
        new Bien(
            "Fideos",
            "",
            10,
            UnidadMedida.KILOGRAMOS,
            hoy.plusYears(1),
            EstadoUso.NO_APLICA,
            subcatFideos);
    Donacion donacion = nuevaDonacion(donanteDummy, List.of(fideos));

    // Act & Assert
    // Intentar iniciar un traslado desde el depósito sin asignar ni planificar
    assertThrows(IllegalStateException.class, () -> donacion.iniciarTraslado("http://test.mapa"));
  }

  @Test
  void retornarAlDeposito_SoloPermitidoDesdeEntregaFallida() {
    Bien fideos =
        new Bien(
            "Fideos",
            "",
            10,
            UnidadMedida.KILOGRAMOS,
            LocalDate.now().plusYears(1),
            EstadoUso.NO_APLICA,
            new SubCategoria("Test", Categoria.ALIMENTO));

    // EnDeposito — estado inicial
    Donacion enDeposito = nuevaDonacion(donanteDummy, List.of(fideos));
    assertThrows(
        IllegalStateException.class,
        enDeposito::retornarAlDeposito,
        "EnDeposito no debería permitir retornarAlDeposito");

    // AsignacionRealizada
    Donacion asignada = nuevaDonacion(donanteDummy, List.of(fideos));
    asignada.confirmarEntidadBeneficiaria(nuevaEntidad("Hogar"));
    assertThrows(
        IllegalStateException.class,
        asignada::retornarAlDeposito,
        "AsignacionRealizada no debería permitir retornarAlDeposito");

    // ListaParaEntregar
    Donacion lista = nuevaDonacion(donanteDummy, List.of(fideos));
    lista.confirmarEntidadBeneficiaria(nuevaEntidad("Hogar"));
    lista.planificarRuta();
    assertThrows(
        IllegalStateException.class,
        lista::retornarAlDeposito,
        "ListaParaEntregar no debería permitir retornarAlDeposito");

    // EnTraslado
    Donacion traslado = nuevaDonacion(donanteDummy, List.of(fideos));
    traslado.confirmarEntidadBeneficiaria(nuevaEntidad("Hogar"));
    traslado.planificarRuta();
    traslado.iniciarTraslado("http://test.mapa");
    assertThrows(
        IllegalStateException.class,
        traslado::retornarAlDeposito,
        "EnTraslado no debería permitir retornarAlDeposito");

    // Entregada
    Donacion entregada = nuevaDonacion(donanteDummy, List.of(fideos));
    entregada.confirmarEntidadBeneficiaria(nuevaEntidad("Hogar"));
    entregada.planificarRuta();
    entregada.iniciarTraslado("http://test.mapa");
    entregada.confirmarEntrega();
    assertThrows(
        IllegalStateException.class,
        entregada::retornarAlDeposito,
        "Entregada no debería permitir retornarAlDeposito");

    // Vencida
    Donacion vencida = nuevaDonacion(donanteDummy, List.of(fideos));
    vencida.marcarVencida();
    assertThrows(
        IllegalStateException.class,
        vencida::retornarAlDeposito,
        "Vencida no debería permitir retornarAlDeposito");
  }

  @Test
  void alRetornarAlDeposito_DespuesDeEntregaFallida_LimpiaEntidadBeneficiaria() {
    // Arrange
    EntidadBeneficiaria entidad = nuevaEntidad("Hogar Test");
    Bien fideos =
        new Bien(
            "Fideos",
            "",
            10,
            UnidadMedida.KILOGRAMOS,
            hoy.plusYears(1),
            EstadoUso.NO_APLICA,
            subcatFideos);
    Donacion donacion = nuevaDonacion(donanteDummy, List.of(fideos));

    // Llevar la donacion hasta EntregaFallida con entidad beneficiaria asignada
    donacion.confirmarEntidadBeneficiaria(
        entidad); // EnDeposito → AsignacionRealizada + set entidad
    assertNotNull(donacion.getEntidadBeneficiaria(), "La entidad debería estar asignada");
    assertEquals(entidad, donacion.getEntidadBeneficiaria());

    donacion.planificarRuta(); // → ListaParaEntregar
    donacion.iniciarTraslado("http://test.mapa"); // → EnTraslado
    donacion.reportarFalla("Dirección incorrecta"); // → EntregaFallida
    assertEquals(EstadoDonacion.ENTREGA_FALLIDA, donacion.getEstado());

    // Act
    donacion.retornarAlDeposito();

    // Assert
    assertEquals(
        EstadoDonacion.EN_DEPOSITO,
        donacion.getEstado(),
        "Tras retornar al depósito, el estado debe ser EnDeposito");
    assertNull(
        donacion.getEntidadBeneficiaria(),
        "La entidad beneficiaria debe limpiarse al retornar al depósito");
  }

  // ==========================================
  // 3. TESTS DE NECESIDADES
  // ==========================================

  @Test
  void entregaFallida_NoPermitePlanificarRutaDirecta() {
    // Arrange
    Bien fideos =
        new Bien(
            "Fideos",
            "",
            10,
            UnidadMedida.KILOGRAMOS,
            hoy.plusYears(1),
            EstadoUso.NO_APLICA,
            subcatFideos);
    Donacion donacion = nuevaDonacion(donanteDummy, List.of(fideos));
    EntidadBeneficiaria entidad = nuevaEntidad("Hogar");

    // Llevar la donacion hasta EntregaFallida
    donacion.confirmarEntidadBeneficiaria(entidad);
    donacion.planificarRuta();
    donacion.iniciarTraslado("http://test.mapa");
    donacion.reportarFalla("No respondieron");
    assertEquals(EstadoDonacion.ENTREGA_FALLIDA, donacion.getEstado());

    // Act & Assert — planificarRuta desde EntregaFallida debe lanzar excepción
    assertThrows(
        IllegalStateException.class,
        donacion::planificarRuta,
        "Desde EntregaFallida no se debe poder planificar ruta directamente");
  }

  @Test
  void necesidades_SumaDonaciones_Y_QuedaSatisfecha() {
    // Arrange
    Necesidad necesidad = new Necesidad(subcatFideos, 5.0, UnidadMedida.KILOGRAMOS, "Fideos");

    // Act & Assert
    necesidad.recibirDonacion(2.0); // Una persona dona 2
    assertFalse(necesidad.estaSatisfecha());

    necesidad.recibirDonacion(20.0); // Otra dona 20, se alcanza el objetivo
    assertTrue(necesidad.estaSatisfecha());

    necesidad.recibirDonacion(8.0); // Mas donaciones, sigue satisfecha
    assertTrue(necesidad.estaSatisfecha());
  }

  // ==========================================
  // 4. TEST DE INVARIANTE: esPerecedero derivado
  // ==========================================

  @Test
  void bien_DerivaPerecederoDeFechaVencimiento() {
    SubCategoria subcat = new SubCategoria("Test", Categoria.ALIMENTO);

    // Caso 1: fechaVencimiento != null → esPerecedero() == true
    Bien conVencimiento =
        new Bien(
            "Con vencimiento",
            "",
            1,
            UnidadMedida.UNIDADES,
            hoy.plusDays(1),
            EstadoUso.NO_APLICA,
            subcat);
    assertTrue(conVencimiento.esPerecedero(), "Si tiene fecha de vencimiento, debe ser perecedero");

    // Caso 2: fechaVencimiento == null → esPerecedero() == false
    Bien sinVencimiento =
        new Bien(
            "Sin vencimiento", "", 1, UnidadMedida.UNIDADES, null, EstadoUso.NO_APLICA, subcat);
    assertFalse(
        sinVencimiento.esPerecedero(), "Si no tiene fecha de vencimiento, no debe ser perecedero");
  }
}
