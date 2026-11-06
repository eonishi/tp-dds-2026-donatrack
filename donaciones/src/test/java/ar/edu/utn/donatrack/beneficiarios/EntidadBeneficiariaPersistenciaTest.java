package ar.edu.utn.donatrack.beneficiarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.donatrack.bienes.Categoria;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.bienes.UnidadMedida;
import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import java.util.List;
import java.util.UUID;
import javax.persistence.PersistenceException;
import org.junit.jupiter.api.Test;

/**
 * Prueba el mapeo del agregado {@link EntidadBeneficiaria} como entidad JPA: la entidad viaja a
 * HSQLDB con sus dos colecciones de necesidades y sus contactos, se limpia el entity manager y se
 * recarga para comprobar que cada colección conserva su contenido y el tipo de sus elementos.
 *
 * <p>Es el pin del defecto heredado de develop: {@code necesidadesExtraordinarias} y {@code
 * necesidadesRecurrentes} compartían la columna {@code entidad_id} mientras {@link
 * NecesidadRecurrente} hereda de {@link Necesidad} en una sola tabla, así que la colección
 * recurrente podía leer una fila {@link Necesidad} y Hibernate levantaba {@code
 * WrongClassException}. Sin esta prueba, la colisión puede volver en silencio.
 */
class EntidadBeneficiariaPersistenciaTest implements SimplePersistenceTest {

  @Test
  void unaEntidadConAmbosTiposDeNecesidadesYCorreosSobreviveAlViajeDeIdaYVuelta() {
    SubCategoria alimentos = new SubCategoria("Alimentos no perecederos", Categoria.ALIMENTO);
    EntidadBeneficiaria entidad =
        new EntidadBeneficiaria("Hogar Test", "Av. Siempreviva 742", "1145557777");
    entidad.agregarNecesidadExtraordinaria(
        new Necesidad(alimentos, 10.0, UnidadMedida.KILOGRAMOS, "Fideos extraordinarios"));
    NecesidadRecurrente recurrente =
        new NecesidadRecurrente(alimentos, 20.0, UnidadMedida.KILOGRAMOS, "Fideos recurrentes");
    recurrente.setPeriodo(TipoPeriodo.MENSUAL);
    entidad.agregarNecesidadRecurrente(recurrente);
    entidad.agregarCorreo(new CorreoElectronico("contacto@hogar.org"));

    persist(alimentos);
    persist(entidad);
    entityManager().flush();
    entityManager().clear();

    EntidadBeneficiaria recargada = find(EntidadBeneficiaria.class, entidad.getId().value());

    assertNotNull(recargada, "La entidad beneficiaria debe poder recargarse por su identidad");

    List<Necesidad> extraordinarias = recargada.getNecesidadesExtraordinarias();
    assertEquals(1, extraordinarias.size(), "La necesidad extraordinaria debe persistirse");
    assertEquals(
        Necesidad.class,
        extraordinarias.get(0).getClass(),
        "La colección extraordinaria no debe contener una necesidad recurrente");
    assertEquals("Fideos extraordinarios", extraordinarias.get(0).getDescripcion());
    assertEquals(10.0, extraordinarias.get(0).getCantidadObjetivo(), 0.001);

    List<NecesidadRecurrente> recurrentes = recargada.getNecesidadesRecurrentes();
    assertEquals(1, recurrentes.size(), "La necesidad recurrente debe persistirse");
    assertInstanceOf(
        NecesidadRecurrente.class,
        recurrentes.get(0),
        "La colección recurrente debe conservar el tipo NecesidadRecurrente");
    assertEquals("Fideos recurrentes", recurrentes.get(0).getDescripcion());
    assertEquals(20.0, recurrentes.get(0).getCantidadObjetivo(), 0.001);
    assertEquals(
        TipoPeriodo.MENSUAL,
        recurrentes.get(0).getPeriodo(),
        "El período de la necesidad recurrente debe conservarse");

    assertEquals(1, recargada.getCorreos().size(), "El correo de la entidad debe persistirse");
    assertEquals("contacto@hogar.org", recargada.getCorreos().get(0).getContacto());
  }

  @Test
  void laColeccionExtraordinariaConservaElTipoRealDeCadaNecesidad() {
    // El dominio acepta cualquier Necesidad en la colección extraordinaria, incluida una
    // recurrente: el tipo real viaja con el objeto y la colección no lo reinterpreta.
    SubCategoria alimentos = new SubCategoria("Alimentos no perecederos", Categoria.ALIMENTO);
    EntidadBeneficiaria entidad =
        new EntidadBeneficiaria("Hogar Polimorfico", "Av. Siempreviva 742", "1145557777");
    NecesidadRecurrente recurrente =
        new NecesidadRecurrente(alimentos, 20.0, UnidadMedida.KILOGRAMOS, "Fideos recurrentes");
    recurrente.setPeriodo(TipoPeriodo.MENSUAL);
    entidad.agregarNecesidadExtraordinaria(recurrente);

    persist(alimentos);
    persist(entidad);
    entityManager().flush();
    entityManager().clear();

    EntidadBeneficiaria recargada = find(EntidadBeneficiaria.class, entidad.getId().value());

    assertNotNull(recargada);
    List<Necesidad> extraordinarias = recargada.getNecesidadesExtraordinarias();
    assertEquals(1, extraordinarias.size());
    assertInstanceOf(
        NecesidadRecurrente.class,
        extraordinarias.get(0),
        "El tipo real de la necesidad debe conservarse dentro de la colección extraordinaria");
    assertEquals(TipoPeriodo.MENSUAL, ((NecesidadRecurrente) extraordinarias.get(0)).getPeriodo());
    assertTrue(
        recargada.getNecesidadesRecurrentes().isEmpty(),
        "La colección recurrente no debe absorber una necesidad que pertenece a la extraordinaria");
  }

  @Test
  void lasColumnasDeUnidadYPeriodoDeLasNecesidadesGuardanElNombreDeLaConstanteYNoElIndice() {
    SubCategoria alimentos = new SubCategoria("Alimentos no perecederos", Categoria.ALIMENTO);
    EntidadBeneficiaria entidad =
        new EntidadBeneficiaria("Hogar Test", "Av. Siempreviva 742", "1145557777");
    entidad.agregarNecesidadExtraordinaria(
        new Necesidad(alimentos, 10.0, UnidadMedida.UNIDADES, "Fideos extraordinarios"));
    NecesidadRecurrente recurrente =
        new NecesidadRecurrente(alimentos, 20.0, UnidadMedida.LITROS, "Fideos recurrentes");
    recurrente.setPeriodo(TipoPeriodo.MENSUAL);
    entidad.agregarNecesidadRecurrente(recurrente);

    persist(alimentos);
    persist(entidad);
    entityManager().flush();
    entityManager().clear();

    EntidadBeneficiaria recargada = find(EntidadBeneficiaria.class, entidad.getId().value());

    assertNotNull(recargada, "La entidad beneficiaria debe poder recargarse por su identidad");
    assertEquals(
        UnidadMedida.UNIDADES,
        recargada.getNecesidadesExtraordinarias().get(0).getUnidad(),
        "La unidad de la necesidad extraordinaria debe sobrevivir al viaje");
    NecesidadRecurrente recurrenteRecargada = recargada.getNecesidadesRecurrentes().get(0);
    assertEquals(
        UnidadMedida.LITROS,
        recurrenteRecargada.getUnidad(),
        "La unidad de la necesidad recurrente debe sobrevivir al viaje");
    assertEquals(
        TipoPeriodo.MENSUAL,
        recurrenteRecargada.getPeriodo(),
        "El período de la necesidad recurrente debe sobrevivir al viaje");

    // Consultas nativas: esquivan el mapeo de Hibernate, leen el valor crudo de cada columna y se
    // restringen por la identidad de cada necesidad de este test para no depender de filas
    // homónimas que otras clases escriban en la base compartida.
    UUID idExtraordinaria = recargada.getNecesidadesExtraordinarias().get(0).getId();
    UUID idRecurrente = recargada.getNecesidadesRecurrentes().get(0).getId();
    Object unidadExtraordinaria =
        entityManager()
            .createNativeQuery("SELECT unidad FROM Necesidad WHERE id = :id")
            .setParameter("id", idExtraordinaria)
            .getSingleResult();
    Object unidadRecurrente =
        entityManager()
            .createNativeQuery("SELECT unidad FROM Necesidad WHERE id = :id")
            .setParameter("id", idRecurrente)
            .getSingleResult();
    Object periodo =
        entityManager()
            .createNativeQuery("SELECT periodo FROM Necesidad WHERE id = :id")
            .setParameter("id", idRecurrente)
            .getSingleResult();

    assertEquals(
        "UNIDADES",
        unidadExtraordinaria,
        "La columna debe guardar el nombre del enum, no su índice");
    assertEquals(
        "LITROS", unidadRecurrente, "La columna debe guardar el nombre del enum, no su índice");
    assertEquals("MENSUAL", periodo, "La columna debe guardar el nombre del enum, no su índice");
  }

  @Test
  void laBaseRechazaInsertarUnaNecesidadConSubcategoriaNula() {
    SubCategoria alimentos =
        new SubCategoria("Necesidad nula " + UUID.randomUUID(), Categoria.ALIMENTO);
    persist(alimentos);
    entityManager().flush();
    Long subcategoriaId =
        (Long)
            entityManager()
                .getEntityManagerFactory()
                .getPersistenceUnitUtil()
                .getIdentifier(alimentos);

    // Inserción nativa de control: la misma sentencia con la FK presente entra, así que el rechazo
    // de la variante nula no puede venir de una columna o un tipo mal escritos. El DDL es el que
    // prueba la restricción, no el chequeo del constructor de Necesidad.
    assertEquals(
        1,
        insertarNecesidadNativa("Control con subcategoría", subcategoriaId),
        "La inserción de control con la FK presente debe entrar");
    assertThrows(
        PersistenceException.class,
        () -> insertarNecesidadNativa("Necesidad sin subcategoría", null),
        "La columna subcategoria_id NOT NULL debe rechazar un insert nativo con null");
  }

  private int insertarNecesidadNativa(String descripcion, Long subcategoriaId) {
    String valorFk = subcategoriaId == null ? "NULL" : ":subcategoriaId";
    var query =
        entityManager()
            .createNativeQuery(
                "INSERT INTO Necesidad (id, DTYPE, descripcion, cantidadRecibida, cantidadObjetivo,"
                    + " unidad, subcategoria_id) VALUES (:id, 'Necesidad', :descripcion, 0, 1,"
                    + " 'UNIDADES', "
                    + valorFk
                    + ")")
            .setParameter("id", UUID.randomUUID())
            .setParameter("descripcion", descripcion);
    if (subcategoriaId != null) {
      query.setParameter("subcategoriaId", subcategoriaId);
    }
    return query.executeUpdate();
  }
}
