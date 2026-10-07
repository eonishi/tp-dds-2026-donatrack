package ar.edu.utn.donatrack.beneficiarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.donatrack.bienes.Categoria;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.bienes.UnidadMedida;
import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Prueba el repositorio de entidades beneficiarias como repositorio JPA: guarda el agregado
 * completo —necesidades extraordinarias, recurrentes y correos—, lo recarga desde HSQLDB y lo
 * elimina por su identidad.
 */
class BeneficiariosRepositoryTest implements SimplePersistenceTest {

  @Test
  void savePersisteElAgregadoCompletoYFindByIdLoRecarga() {
    SubCategoria alimentos = new SubCategoria("Alimentos no perecederos", Categoria.ALIMENTO);
    EntidadBeneficiaria entidad = crearEntidad("Hogar Test");
    entidad.agregarNecesidadExtraordinaria(
        new Necesidad(alimentos, 10.0, UnidadMedida.KILOGRAMOS, "Fideos extraordinarios"));
    NecesidadRecurrente recurrente =
        new NecesidadRecurrente(alimentos, 20.0, UnidadMedida.KILOGRAMOS, "Fideos recurrentes");
    recurrente.setPeriodo(TipoPeriodo.MENSUAL);
    entidad.agregarNecesidadRecurrente(recurrente);
    entidad.agregarCorreo(new CorreoElectronico("contacto@hogar.org"));

    BeneficiariosRepository repositorio = new BeneficiariosRepository();
    // La resolución del catálogo pertenece al caso de uso: acá la subcategoría se siembra antes
    // porque el repositorio de beneficiarios ya no escribe el catálogo y la clave foránea de la
    // necesidad debe existir al persistir la entidad.
    persist(alimentos);
    repositorio.save(entidad);
    entityManager().flush();
    entityManager().clear();

    EntidadBeneficiaria recargada = repositorio.findById(entidad.getId()).orElseThrow();

    assertEquals("Hogar Test", recargada.getRazonSocial());
    assertEquals(1, recargada.getNecesidadesExtraordinarias().size());
    assertEquals(1, recargada.getNecesidadesRecurrentes().size());
    assertInstanceOf(NecesidadRecurrente.class, recargada.getNecesidadesRecurrentes().get(0));
    assertEquals(TipoPeriodo.MENSUAL, recargada.getNecesidadesRecurrentes().get(0).getPeriodo());
    assertEquals(
        "Alimentos no perecederos",
        recargada.getNecesidadesExtraordinarias().get(0).getSubcategoria().getNombre(),
        "La subcategoría compuesta por la API debe quedar almacenada y ligada a la necesidad");
    assertEquals(1, recargada.getCorreos().size());
    assertEquals("contacto@hogar.org", recargada.getCorreos().get(0).getContacto());
  }

  @Test
  void saveActualizaUnaEntidadExistente() {
    EntidadBeneficiaria entidad = crearEntidad("Hogar Test");
    entidad.agregarCorreo(new CorreoElectronico("viejo@hogar.org"));
    BeneficiariosRepository repositorio = new BeneficiariosRepository();
    repositorio.save(entidad);
    entityManager().flush();
    entityManager().clear();

    EntidadBeneficiaria recargada = repositorio.findById(entidad.getId()).orElseThrow();
    recargada.actualizarDatos(
        "Hogar Renombrado",
        "Calle Nueva 456",
        "1199998888",
        List.of(new CorreoElectronico("nuevo@hogar.org")));
    repositorio.save(recargada);
    entityManager().flush();
    entityManager().clear();

    EntidadBeneficiaria actualizada = repositorio.findById(entidad.getId()).orElseThrow();
    assertEquals("Hogar Renombrado", actualizada.getRazonSocial());
    assertEquals("Calle Nueva 456", actualizada.getDireccion());
    assertEquals(1, actualizada.getCorreos().size(), "El contacto reemplazado no debe duplicarse");
    assertEquals("nuevo@hogar.org", actualizada.getCorreos().get(0).getContacto());
  }

  @Test
  void deleteByIdEliminaLaEntidadConSuAgregado() {
    SubCategoria alimentos = new SubCategoria("Alimentos no perecederos", Categoria.ALIMENTO);
    EntidadBeneficiaria entidad = crearEntidad("Hogar Test");
    entidad.agregarNecesidadExtraordinaria(
        new Necesidad(alimentos, 10.0, UnidadMedida.KILOGRAMOS, "Fideos extraordinarios"));
    // La subcategoría se siembra antes porque el repositorio ya no escribe el catálogo.
    persist(alimentos);
    BeneficiariosRepository repositorio = new BeneficiariosRepository();
    repositorio.save(entidad);
    entityManager().flush();
    entityManager().clear();

    repositorio.deleteById(entidad.getId());
    entityManager().flush();
    entityManager().clear();

    assertTrue(
        repositorio.findById(entidad.getId()).isEmpty(),
        "La entidad eliminada no debe poder recargarse");
  }

  private static EntidadBeneficiaria crearEntidad(String razonSocial) {
    return new EntidadBeneficiaria(razonSocial, "Av. Siempreviva 742", "1145557777");
  }
}
