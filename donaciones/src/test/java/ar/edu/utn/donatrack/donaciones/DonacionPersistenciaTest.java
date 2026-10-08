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
import java.time.temporal.ChronoUnit;
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
    SubCategoria alimentos = new SubCategoria(nombreUnicoAlimentos(), Categoria.ALIMENTO);
    Donante donante = crearDonante();
    Bien bien = crearBien(alimentos, "Fideos secos", 5.0);
    Donacion donacion = new Donacion(UUID.randomUUID(), donante, List.of(bien));
    donacion.cambiarEstado(EstadoDonacion.EN_TRASLADO, null);
    donacion.cambiarEstado(EstadoDonacion.ENTREGADA, "Entrega confirmada");
    // Las fechas que la donación fijó en memoria son el valor de referencia: la recarga debe
    // devolver exactamente esas, no otras. Comparar contra ellas cubre el reestampado al hacer
    // flush, el recálculo al leer y el colapso de ambas filas a un mismo instante.
    LocalDateTime fechaPrimera = donacion.getHistorialEstado().get(0).getFecha();
    LocalDateTime fechaSegunda = donacion.getHistorialEstado().get(1).getFecha();
    UUID id = donacion.getId();

    persist(alimentos);
    persist(donante);
    persist(donacion);
    entityManager().flush();
    entityManager().clear();

    Donacion recargada = find(Donacion.class, id);

    assertNotNull(recargada, "La donación debe poder recargarse por su identidad");
    assertEquals(id, recargada.getId(), "La identidad UUID debe conservarse");
    assertEquals(EstadoDonacion.ENTREGADA, recargada.getEstado(), "El estado debe conservarse");
    assertEquals("30111222", recargada.getDonante().getNumDocumento(), "El donante debe ligarse");

    List<HistorialEstado> historial = recargada.getHistorialEstado();
    assertEquals(2, historial.size(), "Las dos transiciones deben persistirse");
    // No hay columna de orden persistente para el historial: el orden recargado depende de que
    // la base devuelva las filas en el orden de inserción.
    assertEquals(EstadoDonacion.EN_TRASLADO, historial.get(0).getEstadoAlcanzado());
    assertNotNull(historial.get(0).getFecha(), "La fecha del primer evento debe persistirse");
    assertEquals(
        fechaPrimera.truncatedTo(ChronoUnit.MICROS),
        historial.get(0).getFecha().truncatedTo(ChronoUnit.MICROS),
        "La fecha del primer evento debe conservarse exactamente");
    assertEquals(EstadoDonacion.ENTREGADA, historial.get(1).getEstadoAlcanzado());
    assertEquals("Entrega confirmada", historial.get(1).getJustificacion());
    assertNotNull(historial.get(1).getFecha(), "La fecha del segundo evento debe persistirse");
    assertEquals(
        fechaSegunda.truncatedTo(ChronoUnit.MICROS),
        historial.get(1).getFecha().truncatedTo(ChronoUnit.MICROS),
        "La fecha del segundo evento debe conservarse exactamente");

    List<Bien> bienes = recargada.getBienes();
    assertEquals(1, bienes.size(), "El bien debe persistirse junto con la donación");
    assertEquals("Fideos secos", bienes.get(0).getDescripcion());
    assertEquals(5.0, bienes.get(0).getCantidad(), 0.001);
    assertEquals(UnidadMedida.KILOGRAMOS, bienes.get(0).getUnidad());
    assertEquals(EstadoUso.NO_APLICA, bienes.get(0).getEstadoDeUso());
    assertEquals(alimentos.getNombre(), bienes.get(0).getSubcategoria().getNombre());
    assertEquals(Categoria.ALIMENTO, bienes.get(0).getSubcategoria().getCategoria());
  }

  @Test
  void lasColumnasDeEstadoGuardanElNombreDeLaConstanteYNoElIndice() {
    SubCategoria alimentos = new SubCategoria(nombreUnicoAlimentos(), Categoria.ALIMENTO);
    Donante donante = crearDonante();
    Donacion donacion =
        new Donacion(UUID.randomUUID(), donante, List.of(crearBien(alimentos, "Arroz", 2.0)));
    donacion.cambiarEstado(EstadoDonacion.EN_TRASLADO, null);
    donacion.cambiarEstado(EstadoDonacion.ENTREGADA, null);

    persist(alimentos);
    persist(donante);
    persist(donacion);
    entityManager().flush();

    // Consultas nativas: esquivan el mapeo de Hibernate y leen el valor crudo de cada columna. Se
    // restringen por la donación sembrada para no depender de las filas que la base compartida ya
    // tenga de otras clases.
    Object estadoDonacion =
        entityManager()
            .createNativeQuery("SELECT estado FROM Donacion WHERE id = :id")
            .setParameter("id", donacion.getId())
            .getSingleResult();
    List<?> estadosAlcanzados =
        entityManager()
            .createNativeQuery(
                "SELECT estadoAlcanzado FROM HistorialEstado WHERE donacion_id = :id ORDER BY fecha")
            .setParameter("id", donacion.getId())
            .getResultList();

    assertEquals(
        "ENTREGADA",
        estadoDonacion,
        "La columna de la donación debe guardar el nombre del enum, no su índice");
    assertEquals(
        List.of("EN_TRASLADO", "ENTREGADA"),
        estadosAlcanzados,
        "Las columnas del historial deben guardar el nombre del enum, no su índice");
  }

  @Test
  void lasColumnasDeUnidadYEstadoDeUsoDeLosBienesGuardanElNombreDeLaConstanteYNoElIndice() {
    SubCategoria mobiliario = new SubCategoria(nombreUnicoMobiliario(), Categoria.MOBILIARIO);
    Donante donante = crearDonante();
    Donacion donacion =
        new Donacion(
            UUID.randomUUID(),
            donante,
            List.of(
                new Bien(
                    "Sillas usadas",
                    "",
                    3.0,
                    UnidadMedida.UNIDADES,
                    null,
                    EstadoUso.USADO,
                    mobiliario)));

    persist(mobiliario);
    persist(donante);
    persist(donacion);
    entityManager().flush();
    entityManager().clear();

    Donacion recargada = find(Donacion.class, donacion.getId());

    assertNotNull(recargada, "La donación debe poder recargarse por su identidad");
    assertEquals(
        UnidadMedida.UNIDADES,
        recargada.getBienes().get(0).getUnidad(),
        "La unidad del bien debe sobrevivir al viaje");
    assertEquals(
        EstadoUso.USADO,
        recargada.getBienes().get(0).getEstadoDeUso(),
        "El estado de uso del bien debe sobrevivir al viaje");

    // Consultas nativas: esquivan el mapeo de Hibernate y leen el valor crudo de cada columna. Se
    // restringen por la donación sembrada para no depender de las filas que la base compartida ya
    // tenga de otras clases, y reducen la lista con findFirst() porque una donación puede contener
    // varios bienes: getSingleResult() sólo sería único por el tamaño de esta fixture.
    Object unidad =
        entityManager()
            .createNativeQuery("SELECT unidad FROM Bien WHERE donacion_id = :id ORDER BY id")
            .setParameter("id", donacion.getId())
            .getResultList()
            .stream()
            .findFirst()
            .orElseThrow();
    Object estadoDeUso =
        entityManager()
            .createNativeQuery("SELECT estadoDeUso FROM Bien WHERE donacion_id = :id ORDER BY id")
            .setParameter("id", donacion.getId())
            .getResultList()
            .stream()
            .findFirst()
            .orElseThrow();

    assertEquals("UNIDADES", unidad, "La columna debe guardar el nombre del enum, no su índice");
    assertEquals("USADO", estadoDeUso, "La columna debe guardar el nombre del enum, no su índice");
  }

  private static String nombreUnicoAlimentos() {
    return "Alimentos no perecederos " + UUID.randomUUID();
  }

  private static String nombreUnicoMobiliario() {
    return "Sillas " + UUID.randomUUID();
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
