package ar.edu.utn.donatrack.donaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.bienes.Categoria;
import ar.edu.utn.donatrack.bienes.EstadoUso;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.bienes.SubcategoriasRepository;
import ar.edu.utn.donatrack.bienes.UnidadMedida;
import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.Genero;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.TipoDocumento;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Fija el defecto que la resolución del catálogo existe para eliminar: una carga con dos bienes que
 * nombran la misma subcategoría debe insertar una sola fila de catálogo, aunque los bienes
 * segmenten en donaciones distintas.
 *
 * <p>Cada prueba usa un nombre único para leer el resultado sin depender de las filas que la base
 * en memoria compartida ya tiene de otras clases. Los bienes se construyen con la misma caché de la
 * solicitud que el controlador arma antes de resolverlos; sin esa caché, cada bien llevaría su
 * propia instancia y la carga insertaría dos filas.
 */
class ResolucionSubcategoriasTest implements SimplePersistenceTest {

  @Test
  void dosBienesConElMismoNombreEnUnaCargaProducenUnaSolaFilaDeCatalogo() {
    String nombre = "Resolucion-" + UUID.randomUUID();
    Donante donante =
        new PersonaHumana(
            new CorreoElectronico("resolucion@test.com"),
            "Ana",
            "Catálogo",
            "20000002",
            TipoDocumento.DNI,
            Genero.FEMENINO,
            "Calle Resolución 100");
    persist(donante);

    SubcategoriasRepository repositorio = new SubcategoriasRepository();
    Map<String, SubCategoria> cache = new HashMap<>();
    SubCategoria sillaNueva = repositorio.obtenerOrCrear(nombre, Categoria.MOBILIARIO, cache);
    SubCategoria sillaUsada = repositorio.obtenerOrCrear(nombre, Categoria.MOBILIARIO, cache);
    List<Bien> bienes =
        List.of(
            bien("Silla nueva", EstadoUso.NUEVO, sillaNueva),
            bien("Silla usada", EstadoUso.USADO, sillaUsada));

    List<Donacion> donaciones =
        new CargaDonacion(new DonacionesRepository()).cargarDonacion(donante, bienes);
    entityManager().flush();

    assertEquals(2, donaciones.size(), "Cada estado de uso debe segmentar su propia donación");
    assertEquals(1, filasConNombre(nombre), "La carga debe insertar una sola fila de catálogo");

    SubCategoria deUnaSegundaSolicitud =
        new SubcategoriasRepository().obtenerOrCrear(nombre, Categoria.VESTIMENTA, new HashMap<>());
    entityManager().flush();

    assertEquals(
        Categoria.MOBILIARIO,
        deUnaSegundaSolicitud.getCategoria(),
        "La segunda solicitud debe recibir la categoría del catálogo");
    assertEquals(1, filasConNombre(nombre), "La segunda solicitud no debe insertar otra fila");
  }

  private static Bien bien(String descripcion, EstadoUso estadoUso, SubCategoria subcategoria) {
    return new Bien(descripcion, "", 1, UnidadMedida.UNIDADES, null, estadoUso, subcategoria);
  }

  private long filasConNombre(String nombre) {
    return createQuery("select s from SubCategoria s where s.nombre = :nombre", SubCategoria.class)
        .setParameter("nombre", nombre)
        .getResultList()
        .size();
  }
}
