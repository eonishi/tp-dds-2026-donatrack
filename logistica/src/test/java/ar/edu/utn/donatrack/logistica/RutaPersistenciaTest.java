package ar.edu.utn.donatrack.logistica;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Prueba el mapeo JPA de {@link Ruta} y {@link Destino} de punta a punta: persiste el agregado,
 * verifica que el proveedor asigne las claves, limpia el contexto, lo recarga y comprueba que la
 * colección de donaciones de cada destino sobreviva con los mismos UUID.
 *
 * <p>Es la evidencia de runtime de que los mapeos de la unidad de persistencia arrancan y de que la
 * tabla {@code logistica.destino_donaciones} se genera con las columnas del mapeo.
 */
class RutaPersistenciaTest implements SimplePersistenceTest {

  @Test
  void persistirYRecargarRuta_ElProveedorAsignaIdsYLaColeccionDeDonacionesSobrevive() {
    Camion camion = new Camion("ABC123", 45.0, 2.5, 5000.0);
    UUID primeraDonacion = UUID.randomUUID();
    UUID segundaDonacion = UUID.randomUUID();
    Destino destino =
        new Destino(7L, "Av. Siempreviva 742", List.of(primeraDonacion, segundaDonacion));
    Ruta ruta = new Ruta(camion, List.of(destino));

    assertNull(ruta.getId(), "La ruta recién construida no tiene id: lo asigna el proveedor");
    assertNull(destino.getId(), "El destino recién construido no tiene id: lo asigna el proveedor");

    persist(camion);
    persist(ruta);
    entityManager().flush();

    UUID idRuta = ruta.getId();
    Long idDestino = destino.getId();
    assertNotNull(idRuta, "El proveedor debe asignar el UUID de la ruta al persistirla");
    assertNotNull(idDestino, "El proveedor debe asignar el id del destino al persistirla");

    entityManager().clear();

    Ruta recargada = find(Ruta.class, idRuta);
    assertNotNull(recargada, "La ruta persistida debe poder recargarse por su UUID");
    assertEquals(idRuta, recargada.getId(), "El UUID asignado debe sobrevivir la recarga");
    assertEquals("ABC123", recargada.getCamion().getPatente());
    assertEquals(1, recargada.getDestinos().size());

    Destino destinoRecargado = recargada.getDestinos().get(0);
    assertEquals(idDestino, destinoRecargado.getId());
    assertEquals(7L, destinoRecargado.entidadId());
    assertEquals("Av. Siempreviva 742", destinoRecargado.direccion());
    assertEquals(
        Set.of(primeraDonacion, segundaDonacion),
        new HashSet<>(destinoRecargado.donaciones()),
        "La colección destino_donaciones debe conservar los UUID persistidos en la recarga");
  }

  @Test
  void elEsquemaDeDestinoDonacionesExponeLasColumnasDelMapeo() {
    List<?> columnas =
        entityManager()
            .createNativeQuery(
                "select column_name from information_schema.columns"
                    + " where lower(table_schema) = 'logistica'"
                    + " and lower(table_name) = 'destino_donaciones'")
            .getResultList();

    assertEquals(
        Set.of("DESTINO_ID", "DONACION_ID"),
        new HashSet<>(columnas),
        "La tabla de elementos debe tener las columnas del @JoinColumn y del @Column del mapeo");
  }
}
