package ar.edu.utn.donatrack;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.donatrack.beneficiarios.*;
import ar.edu.utn.donatrack.bienes.*;
import ar.edu.utn.donatrack.donaciones.*;
import ar.edu.utn.donatrack.estados.*;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DonaTrackTest {

  private SubCategoria subcatSillas;
  private SubCategoria subcatFideos;
  private LocalDate hoy;

  @BeforeEach
  void setUp() {
    subcatSillas = new SubCategoria("Sillas", Categoria.MOBILIARIO);
    subcatFideos = new SubCategoria("Fideos Secos", Categoria.ALIMENTO);
    hoy = LocalDate.now();
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
    CargaDonacion carga = new CargaDonacion(hoy);

    // Act
    carga.cargarDonacion(bienesDonados);

    // Assert
    // Debe haber 4 donaciones separadas:
    // 1. Sillas Nuevas
    // 2. Sillas Usadas
    // 3. Fideos 2026 (agrupando 'fideos2026' y 'masFideos2026')
    // 4. Fideos 2027
    assertEquals(
        4,
        carga.getDonaciones().size(),
        "La segmentación no agrupó correctamente las unidades mínimas.");

    // Verificamos que agrupó correctamente los fideos del mismo vencimiento
    boolean encontroGrupoFideos =
        carga.getDonaciones().stream()
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
    Donacion donacion = new Donacion(List.of(fideos));

    // Act & Assert
    // Estado inicial
    assertInstanceOf(EnDeposito.class, donacion.getEstado());

    // Transición: En Depósito -> Asignada
    donacion.asignar();
    assertInstanceOf(AsignacionRealizada.class, donacion.getEstado());

    // Transición: Asignada -> Lista para entregar
    donacion.planificarRuta();
    assertInstanceOf(ListaParaEntregar.class, donacion.getEstado());

    // Transición: Lista para entregar -> En traslado
    donacion.iniciarTraslado();
    assertInstanceOf(EnTraslado.class, donacion.getEstado());

    // Transición: En traslado -> Entrega fallida (Único estado que requiere justificación según
    // enunciado)
    donacion.reportarFalla("Tocamos timbre pero nadie respondió");
    assertInstanceOf(EntregaFallida.class, donacion.getEstado());

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
    Donacion donacion = new Donacion(List.of(fideos));

    // Act & Assert
    // Intentar iniciar un traslado desde el depósito sin asignar ni planificar
    assertThrows(IllegalStateException.class, donacion::iniciarTraslado);
  }

  // ==========================================
  // 3. TESTS DE NECESIDADES
  // ==========================================

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
