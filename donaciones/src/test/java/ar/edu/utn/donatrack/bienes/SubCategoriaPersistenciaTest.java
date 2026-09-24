package ar.edu.utn.donatrack.bienes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import org.junit.jupiter.api.Test;

/**
 * Prueba que {@link SubCategoria} es una entrada del catálogo compartido entre bienes y
 * necesidades: su categoría viaja a la base como el nombre de la constante y su identidad de
 * dominio es el nombre, no el id que la base asigna al persistir.
 *
 * <p>La igualdad por nombre es la que hace que dos referencias al mismo nombre resuelvan a la misma
 * entrada; el id queda afuera porque la columna IDENTITY lo asigna recién al insertar, así que un
 * hash calculado con él mutaría en el momento en que la fila se persiste.
 *
 * <p>Las recargas y las consultas nativas se restringen por id y no por nombre: la base en memoria
 * es única por JVM y las pruebas que confirman su transacción dejan filas de otras clases con
 * nombres repetidos, así que "Sillas" no identifica una fila por sí solo.
 */
class SubCategoriaPersistenciaTest implements SimplePersistenceTest {

  @Test
  void laCategoriaDeUnaSubcategoriaSeGuardaComoNombreYNoComoIndice() {
    SubCategoria sillas = new SubCategoria("Sillas", Categoria.MOBILIARIO);
    Long id = persistirYDesligar(sillas);

    SubCategoria recargada = find(SubCategoria.class, id);

    assertNotNull(recargada, "La subcategoría debe poder recargarse por su identidad");
    assertEquals(
        Categoria.MOBILIARIO, recargada.getCategoria(), "La categoría debe sobrevivir al viaje");
    // Consulta nativa: esquiva el mapeo de Hibernate y lee el valor crudo de la columna.
    assertEquals(
        "MOBILIARIO",
        categoriaCruda(id),
        "La columna debe guardar el nombre del enum, no su índice");
  }

  @Test
  void dosSubcategoriasConElMismoNombreSonIgualesYCompartenHashCodeAunqueCambieSuId() {
    SubCategoria separada = new SubCategoria("Sillas", Categoria.MOBILIARIO);
    Long id = persistirYDesligar(new SubCategoria("Sillas", Categoria.MOBILIARIO));

    SubCategoria recargada = find(SubCategoria.class, id);

    assertEquals(separada, recargada, "El mismo nombre debe ser la misma entrada del catálogo");
    assertEquals(recargada, separada, "La igualdad no debe depender del id que generó la base");
    assertEquals(
        separada.hashCode(), recargada.hashCode(), "El id de la fila no debe participar del hash");
  }

  @Test
  void dosSubcategoriasConNombresDistintosNoSonIguales() {
    SubCategoria sillas = new SubCategoria("Sillas", Categoria.MOBILIARIO);
    SubCategoria mesas = new SubCategoria("Mesas", Categoria.MOBILIARIO);

    assertNotEquals(sillas, mesas, "Nombres distintos no son la misma entrada del catálogo");
  }

  @Test
  void elHashCodeDeUnaSubcategoriaNoCambiaAlPersistirseYRecargarse() {
    SubCategoria sillas = new SubCategoria("Sillas", Categoria.MOBILIARIO);
    int hashSeparado = sillas.hashCode();

    persist(sillas);
    entityManager().flush();

    assertEquals(
        hashSeparado,
        sillas.hashCode(),
        "El id asignado a la fila no debe alterar el hash de la instancia");

    entityManager().clear();
    SubCategoria recargada = find(SubCategoria.class, identificador(sillas));

    assertEquals(hashSeparado, recargada.hashCode(), "El hash debe sobrevivir al viaje");
    assertEquals(sillas, recargada, "La instancia recargada sigue siendo la misma entrada");
  }

  @Test
  void unBienConSubcategoriaNulaNoSePuedeConstruir() {
    assertThrows(
        NullPointerException.class,
        () -> new Bien("Silla", "", 1.0, UnidadMedida.UNIDADES, null, EstadoUso.NUEVO, null));
  }

  private Long persistirYDesligar(SubCategoria subcategoria) {
    persist(subcategoria);
    entityManager().flush();
    Long id = identificador(subcategoria);
    entityManager().clear();
    return id;
  }

  private Long identificador(SubCategoria subcategoria) {
    return (Long)
        entityManager()
            .getEntityManagerFactory()
            .getPersistenceUnitUtil()
            .getIdentifier(subcategoria);
  }

  private Object categoriaCruda(Long id) {
    return entityManager()
        .createNativeQuery("SELECT categoria FROM SubCategoria WHERE id = :id")
        .setParameter("id", id)
        .getSingleResult();
  }
}
