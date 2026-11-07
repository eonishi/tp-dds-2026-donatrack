package ar.edu.utn.donatrack.donaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Fija el invariante de atomicidad de una carga multipropósito: si una segmentación falla, ninguna
 * de las donaciones ya persistidas en la misma carga debe quedar almacenada.
 *
 * <p>Antes lo garantizaba el borde transaccional por solicitud; después del refactor lo garantiza
 * {@code withTransaction} en el caso de uso. No usa {@code SimplePersistenceTest} porque su
 * {@code @BeforeEach} abre una transacción que el {@code withTransaction} de producción
 * confirmaría: acá cada siembra y cada carga abren su propia transacción explícita.
 */
class CargaDonacionAtomicidadTest implements WithSimplePersistenceUnit {

  private static final String FALLA_SIMULADA = "falla simulada";

  private final List<SubCategoria> subcategorias = new ArrayList<>();
  private Donante donante;

  @Test
  void unaFallaEnLaTerceraSegmentacionRevierteTodasLasDonaciones() {
    sembrarDatosBase();

    CargaDonacion carga = new CargaDonacion(new RepositorioEspia());
    List<Bien> bienes =
        List.of(
            bien("Bien A", subcategorias.get(0)),
            bien("Bien B", subcategorias.get(1)),
            bien("Bien C", subcategorias.get(2)));

    IllegalStateException error =
        assertThrows(
            IllegalStateException.class,
            () -> withTransaction(() -> carga.cargarDonacion(donante, bienes)));

    assertEquals(FALLA_SIMULADA, error.getMessage());
    entityManager().clear();
    assertTrue(
        donacionesDelDonante().isEmpty(),
        "El rollback debe eliminar también las donaciones del donante ya persistidas antes de la"
            + " falla");
  }

  /** Donaciones persistidas del donante sembrado por este test. */
  private List<Donacion> donacionesDelDonante() {
    return createQuery("select d from Donacion d where d.donante.id = :donanteId", Donacion.class)
        .setParameter("donanteId", donante.getId())
        .getResultList();
  }

  private void sembrarDatosBase() {
    donante =
        new PersonaHumana(
            new CorreoElectronico("atomicidad@test.com"),
            "Ana",
            "Atómica",
            "10000001",
            TipoDocumento.DNI,
            Genero.FEMENINO,
            "Calle Atómica 100");
    subcategorias.add(new SubCategoria("Atomicidad-Alimento", Categoria.ALIMENTO));
    subcategorias.add(new SubCategoria("Atomicidad-Mobiliario", Categoria.MOBILIARIO));
    subcategorias.add(new SubCategoria("Atomicidad-Vestimenta", Categoria.VESTIMENTA));
    withTransaction(
        () -> {
          persist(donante);
          subcategorias.forEach(this::persist);
        });
  }

  private static Bien bien(String descripcion, SubCategoria subcategoria) {
    return new Bien(
        descripcion, "", 1, UnidadMedida.UNIDADES, null, EstadoUso.NO_APLICA, subcategoria);
  }

  /** Borra lo que el test confirmó para no contaminar el HSQLDB en memoria compartido. */
  @AfterEach
  void limpiarDatosSembrados() {
    if (donante == null) {
      return;
    }
    entityManager().clear();
    withTransaction(
        () -> {
          donacionesDelDonante().forEach(this::remove);
          subcategorias.forEach(this::eliminarSubcategoria);
          Donante almacenado = find(Donante.class, donante.getId());
          if (almacenado != null) {
            remove(almacenado);
          }
        });
  }

  private void eliminarSubcategoria(SubCategoria subcategoria) {
    SubCategoria almacenada =
        createQuery("select s from SubCategoria s where s.nombre = :nombre", SubCategoria.class)
            .setParameter("nombre", subcategoria.getNombre())
            .getResultList()
            .stream()
            .findFirst()
            .orElse(null);
    if (almacenada != null) {
      remove(almacenada);
    }
  }

  /** Repositorio de donaciones que delega las dos primeras altas y falla en la tercera. */
  private static final class RepositorioEspia extends DonacionesRepository {
    private int llamadas;

    @Override
    public void addDonacion(Donacion donacion) {
      llamadas++;
      if (llamadas == 3) {
        throw new IllegalStateException(FALLA_SIMULADA);
      }
      super.addDonacion(donacion);
      // El flush manda las altas delegadas a la base dentro de la misma transacción: así el
      // rollback tiene filas reales que revertir y la aserción no puede pasar sobre un no-op.
      entityManager().flush();
    }
  }
}
