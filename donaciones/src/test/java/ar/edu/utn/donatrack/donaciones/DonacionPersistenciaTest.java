package ar.edu.utn.donatrack.donaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Prueba de punta a punta del agregado Donacion como entidad JPA: la donación viaja a HSQLDB, se
 * limpia el entity manager y se recarga por su identidad para comprobar que el estado, el historial
 * y los bienes sobrevivieron al viaje.
 */
class DonacionPersistenciaTest implements SimplePersistenceTest {

  @Test
  void unaDonacionConHistorialYBienesSobreviveAlViajeDeIdaYVuelta() {
    SubCategoria alimentos = new SubCategoria("Alimentos no perecederos", Categoria.ALIMENTO);
    Donante donante = crearDonante();
    Bien bien = crearBien(alimentos, "Fideos secos", 5.0);
    Donacion donacion = new Donacion(DonacionId.generar(), donante, List.of(bien));
    LocalDateTime fechaTraslado = LocalDateTime.of(2026, 1, 10, 9, 30);
    LocalDateTime fechaEntrega = LocalDateTime.of(2026, 1, 11, 10, 45);
    donacion.cambiarEstado(
        new HistorialEstado(fechaTraslado, EstadoDonacion.EN_TRASLADO, null),
        EstadoDonacion.EN_TRASLADO);
    donacion.cambiarEstado(
        new HistorialEstado(fechaEntrega, EstadoDonacion.ENTREGADA, "Entrega confirmada"),
        EstadoDonacion.ENTREGADA);
    UUID id = donacion.getId().value();

    persist(alimentos);
    persist(donante);
    persist(donacion);
    entityManager().flush();
    entityManager().clear();

    Donacion recargada = find(Donacion.class, id);

    assertNotNull(recargada, "La donación debe poder recargarse por su identidad");
    assertEquals(new DonacionId(id), recargada.getId(), "La identidad UUID debe conservarse");
    assertEquals(EstadoDonacion.ENTREGADA, recargada.getEstado(), "El estado debe conservarse");
    assertEquals("30111222", recargada.getDonante().getNumDocumento(), "El donante debe ligarse");

    List<HistorialEstado> historial = recargada.getHistorialEstado();
    assertEquals(2, historial.size(), "Las dos transiciones deben persistirse");
    assertEquals(1L, historial.get(0).getSecuencia(), "La primera entrada conserva su secuencia");
    assertEquals(
        EstadoDonacion.EN_DEPOSITO,
        historial.get(0).getEstadoAnterior(),
        "La primera transición parte del depósito");
    assertEquals(EstadoDonacion.EN_TRASLADO, historial.get(0).getEstadoAlcanzado());
    assertEquals(
        fechaTraslado, historial.get(0).getFecha(), "La fecha del evento debe conservarse");
    assertEquals(2L, historial.get(1).getSecuencia(), "La segunda entrada conserva su secuencia");
    assertEquals(EstadoDonacion.EN_TRASLADO, historial.get(1).getEstadoAnterior());
    assertEquals(EstadoDonacion.ENTREGADA, historial.get(1).getEstadoAlcanzado());
    assertEquals("Entrega confirmada", historial.get(1).getJustificacion());
    assertEquals(fechaEntrega, historial.get(1).getFecha(), "La fecha del evento debe conservarse");

    List<Bien> bienes = recargada.getBienes();
    assertEquals(1, bienes.size(), "El bien debe persistirse junto con la donación");
    assertEquals("Fideos secos", bienes.get(0).getDescripcion());
    assertEquals(5.0, bienes.get(0).getCantidad(), 0.001);
    assertEquals(UnidadMedida.KILOGRAMOS, bienes.get(0).getUnidad());
    assertEquals(EstadoUso.NO_APLICA, bienes.get(0).getEstadoDeUso());
    assertEquals("Alimentos no perecederos", bienes.get(0).getSubcategoria().getNombre());
    assertEquals(Categoria.ALIMENTO, bienes.get(0).getSubcategoria().getCategoria());
  }

  @Test
  void lasColumnasDeEstadoGuardanElNombreDeLaConstanteYNoElIndice() {
    SubCategoria alimentos = new SubCategoria("Alimentos no perecederos", Categoria.ALIMENTO);
    Donante donante = crearDonante();
    Donacion donacion =
        new Donacion(DonacionId.generar(), donante, List.of(crearBien(alimentos, "Arroz", 2.0)));
    donacion.cambiarEstado(
        new HistorialEstado(LocalDateTime.now(), EstadoDonacion.EN_TRASLADO, null),
        EstadoDonacion.EN_TRASLADO);
    donacion.cambiarEstado(
        new HistorialEstado(LocalDateTime.now(), EstadoDonacion.ENTREGADA, null),
        EstadoDonacion.ENTREGADA);

    persist(alimentos);
    persist(donante);
    persist(donacion);
    entityManager().flush();

    // Consultas nativas: esquivan el mapeo de Hibernate y leen el valor crudo de cada columna.
    Object estadoDonacion =
        entityManager().createNativeQuery("SELECT estado FROM Donacion").getSingleResult();
    List<?> estadosAlcanzados =
        entityManager()
            .createNativeQuery("SELECT estadoAlcanzado FROM HistorialEstado ORDER BY secuencia")
            .getResultList();
    Object estadoAnteriorInicial =
        entityManager()
            .createNativeQuery("SELECT estadoAnterior FROM HistorialEstado WHERE secuencia = 1")
            .getSingleResult();

    assertEquals(
        "ENTREGADA",
        estadoDonacion,
        "La columna de la donación debe guardar el nombre del enum, no su índice");
    assertEquals(
        List.of("EN_TRASLADO", "ENTREGADA"),
        estadosAlcanzados,
        "Las columnas del historial deben guardar el nombre del enum, no su índice");
    assertEquals(
        "EN_DEPOSITO",
        estadoAnteriorInicial,
        "La columna del estado anterior también debe guardar el nombre del enum");
  }

  private static Donante crearDonante() {
    return new PersonaHumana(
        new CorreoElectronico("ana@test.com"),
        "Ana",
        "Gomez",
        34,
        "30111222",
        TipoDocumento.DNI,
        Genero.FEMENINO,
        "Av. Siempreviva 742");
  }

  private static Bien crearBien(SubCategoria subcategoria, String descripcion, double cantidad) {
    return new Bien(
        descripcion,
        "",
        cantidad,
        UnidadMedida.KILOGRAMOS,
        LocalDate.now().plusMonths(6),
        EstadoUso.NO_APLICA,
        subcategoria);
  }
}
