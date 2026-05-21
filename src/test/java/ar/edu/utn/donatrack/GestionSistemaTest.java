package ar.edu.utn.donatrack;

import ar.edu.utn.donatrack.BeneficiariosYNecesidades.*;
import ar.edu.utn.donatrack.BienesYCategorias.*;
import ar.edu.utn.donatrack.DonantesYContacto.*;
import ar.edu.utn.donatrack.EstadoDeDonacion.*;
import ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones.*;
import ar.edu.utn.donatrack.ServiciosExternosYUtilidades.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DonaTrackTest {

    private SubCategoria subcatSillas;
    private SubCategoria subcatFideos;
    private LocalDate hoy;

    @BeforeEach
    void setUp() {
        subcatSillas = new SubCategoria("Sillas");
        subcatFideos = new SubCategoria("Fideos Secos");
        hoy = LocalDate.now();
    }

    // ==========================================
    // 1. TESTS DE SEGMENTACIÓN DE DONACIONES
    // ==========================================

    @Test
    void segmentarDonacion_AgrupaPorSubcategoria_EstadoUso_Y_Vencimiento() {
        // Arrange
        Bien sillaNueva = new Bien("Silla nueva", "", 2, UnidadMedida.UNIDADES, false, null, EstadoUso.NUEVO, subcatSillas);
        Bien sillaUsada = new Bien("Silla usada", "", 4, UnidadMedida.UNIDADES, false, null, EstadoUso.USADO, subcatSillas);
        Bien fideos2026 = new Bien("Fideos que vencen 2026", "", 10, UnidadMedida.KILOGRAMOS, true, hoy.plusYears(1), EstadoUso.NO_APLICA, subcatFideos);
        Bien fideos2027 = new Bien("Fideos que vencen 2027", "", 5, UnidadMedida.KILOGRAMOS, true, hoy.plusYears(2), EstadoUso.NO_APLICA, subcatFideos);
        Bien masFideos2026 = new Bien("Más fideos mismo lote", "", 5, UnidadMedida.KILOGRAMOS, true, hoy.plusYears(1), EstadoUso.NO_APLICA, subcatFideos);

        List<Bien> bienesDonados = Arrays.asList(sillaNueva, sillaUsada, fideos2026, fideos2027, masFideos2026);
        CargaDonacion carga = new CargaDonacion(hoy);

        // Act
        carga.cargarDonacion(bienesDonados);

        // Assert
        // Debe haber 4 donaciones separadas:
        // 1. Sillas Nuevas
        // 2. Sillas Usadas
        // 3. Fideos 2026 (agrupando 'fideos2026' y 'masFideos2026')
        // 4. Fideos 2027
        assertEquals(4, carga.getDonaciones().size(), "La segmentación no agrupó correctamente las unidades mínimas.");
        
        // Verificamos que agrupó correctamente los fideos del mismo vencimiento
        boolean encontroGrupoFideos = carga.getDonaciones().stream()
                .anyMatch(d -> d.getBienes().size() == 2 && d.getSubcategoria().equals(subcatFideos));
        assertTrue(encontroGrupoFideos, "No agrupó los bienes idénticos en la misma donación.");
    }

    // ==========================================
    // 2. TESTS DE LA MÁQUINA DE ESTADOS
    // ==========================================

    @Test
    void estadosDonacion_TransicionesValidas_Y_Falla() {
        // Arrange
        Bien fideos = new Bien("Fideos", "", 10, UnidadMedida.KILOGRAMOS, true, hoy.plusYears(1), EstadoUso.NO_APLICA, subcatFideos);
        Donacion donacion = new Donacion(List.of(fideos));

        // Act & Assert
        // Estado inicial
        assertTrue(donacion.getEstado() instanceof EnDeposito);

        // Transición: En Depósito -> Asignada
        donacion.asignar();
        assertTrue(donacion.getEstado() instanceof AsignacionRealizada);

        // Transición: Asignada -> Lista para entregar
        donacion.planificarRuta();
        assertTrue(donacion.getEstado() instanceof ListaParaEntregar);

        // Transición: Lista para entregar -> En traslado
        donacion.iniciarTraslado();
        assertTrue(donacion.getEstado() instanceof EnTraslado);

        // Transición: En traslado -> Entrega fallida (Único estado que requiere justificación según enunciado)
        donacion.reportarFalla("Tocamos timbre pero nadie respondió");
        assertTrue(donacion.getEstado() instanceof EntregaFallida);

        // Verificamos el historial para asegurar la trazabilidad
        List<HistorialEstado> historial = donacion.getHistorialEstado();
        assertEquals(4, historial.size(), "El historial no registró todas las transiciones.");
        assertEquals("Tocamos timbre pero nadie respondió", historial.get(3).getJustificacion());
    }

    @Test
    void estadosDonacion_TransicionInvalidaLanzaExcepcion() {
        // Arrange
        Bien fideos = new Bien("Fideos", "", 10, UnidadMedida.KILOGRAMOS, true, hoy.plusYears(1), EstadoUso.NO_APLICA, subcatFideos);
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
        NecesidadExtraordinaria necesidad = new NecesidadExtraordinaria();
        necesidad.setCantidadObjetivo(30.0); // Ejemplo: 30 bancos de la escuela rural [cite: 72]

        // Act & Assert
        necesidad.recibirDonacion(2.0); // Una persona dona 2 [cite: 73]
        assertFalse(necesidad.estaSatisfecha());

        necesidad.recibirDonacion(20.0); // Otra dona 20 [cite: 73]
        assertFalse(necesidad.estaSatisfecha());

        necesidad.recibirDonacion(8.0); // Se alcanza el objetivo [cite: 74]
        assertTrue(necesidad.estaSatisfecha());
    }

    // ==========================================
    // 4. TESTS DE REPOSITORIO E IMPORTACIÓN
    // ==========================================

    @Test
    void repositorioCSV_EvitaDuplicadosPorEmail_Y_Actualiza() {
        // Arrange
        DonantesRepository repo = new DonantesRepository();
        
        // Creamos un donante inicial en el sistema
        CorreoElectronico mail = new CorreoElectronico("ana@mail.com");
        PersonaHumana existente = new PersonaHumana(mail, "Ana", "Gomez", 0, 1111, TipoDocumento.DNI, "", "");
        repo.registrarOActualizar(existente);

        // Simulamos un CSV donde viene la misma Ana pero con apellido actualizado ("Pérez") y número de doc [cite: 85]
        String csvData = "TipoPersona,TipoDoc,Documento,Nombre/Razón Social,Email,Teléfono\n" +
                         "HUMANA,DNI,12345678,Ana Pérez,ana@mail.com,+54 11 5555-5555";
        StringReader reader = new StringReader(csvData);

        // Act
        ImportadorDonantesCSV.importarYRegistrar(reader, repo);

        // Assert
        assertEquals(1, repo.getDonantes().size(), "El repositorio duplicó al donante en vez de actualizarlo.");
        
        PersonaHumana actualizada = (PersonaHumana) repo.getDonantes().get(0);
        assertEquals("Pérez", actualizada.getApellido(), "No se actualizó el apellido del donante existente.");
        assertEquals(12345678L, actualizada.getNumDocumento(), "No se actualizó el documento.");
        assertEquals(2, actualizada.getMediosDeContacto().size(), "No se agregó el nuevo teléfono de contacto.");
    }
}