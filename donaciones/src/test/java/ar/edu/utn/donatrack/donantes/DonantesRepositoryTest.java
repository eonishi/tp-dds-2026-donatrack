package ar.edu.utn.donatrack.donantes;

import static ar.edu.utn.donatrack.testsupport.FabricaDePrueba.nuevaDonacion;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.bienes.Categoria;
import ar.edu.utn.donatrack.bienes.EstadoUso;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.bienes.UnidadMedida;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.EstadoDonacion;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Prueba el repositorio de donantes como repositorio JPA: guarda cada tipo de donante con sus
 * medios de contacto, limpia el contexto de persistencia, lo recarga por su identidad y por el
 * listado, y lo elimina. Cada recarga usa un repositorio nuevo para que la lectura tenga que salir
 * de la base y no de una colección en memoria.
 */
class DonantesRepositoryTest implements SimplePersistenceTest {

  @Test
  void saveRecargaYEliminaUnaPersonaHumana() {
    PersonaHumana donante =
        new PersonaHumana(
            new CorreoElectronico("ana@test.com"),
            "Ana",
            "Gomez",
            34,
            "30111222",
            TipoDocumento.DNI,
            Genero.FEMENINO,
            "Av. Siempreviva 742");
    donante.addMedioDeContacto(new Sms("1145558888"));
    donante.addMedioDeContacto(new Whatsapp("1145559999"));
    UUID id = donante.getId();

    new DonantesRepository().save(donante);
    entityManager().flush();
    entityManager().clear();

    DonantesRepository repositorio = new DonantesRepository();
    Donante porIdentidad = repositorio.findById(id).orElseThrow();

    assertInstanceOf(PersonaHumana.class, porIdentidad, "El subtipo humano debe conservarse");
    PersonaHumana persona = (PersonaHumana) porIdentidad;
    assertEquals("Ana", persona.getNombre());
    assertEquals("Gomez", persona.getApellido());
    assertEquals(34, persona.getEdad());
    assertEquals(Genero.FEMENINO, persona.getGenero());
    assertEquals("Av. Siempreviva 742", persona.getDireccion());
    assertEquals("30111222", persona.getNumDocumento());
    assertEquals(TipoDocumento.DNI, persona.getDocumento());
    assertEquals(
        "ana@test.com",
        persona.extraerEmail().orElseThrow(),
        "El correo debe recuperarse desde la base");
    assertEquals(
        "ana@test.com",
        persona.getMedioContactoPredeterminado().getContacto(),
        "El contacto predeterminado debe sobrevivir al viaje");

    var listado = repositorio.getDonantes();
    assertEquals(1, listado.size(), "El donante guardado debe aparecer en el listado");
    Donante porListado =
        listado.stream()
            .filter(d -> d.getId().equals(id))
            .findFirst()
            .orElseThrow(
                () -> new AssertionError("El donante debe aparecer en el listado del repositorio"));
    assertInstanceOf(PersonaHumana.class, porListado, "El listado debe conservar el subtipo");
    assertEquals(
        3,
        porListado.getMediosDeContacto().size(),
        "Los tres canales de contacto deben persistirse");
    assertTrue(
        porListado.getMediosDeContacto().stream().anyMatch(Sms.class::isInstance), "Falta el SMS");
    assertTrue(
        porListado.getMediosDeContacto().stream().anyMatch(Whatsapp.class::isInstance),
        "Falta el WhatsApp");

    repositorio.deleteById(id);
    entityManager().flush();
    entityManager().clear();

    DonantesRepository vacio = new DonantesRepository();
    assertTrue(
        vacio.findById(id).isEmpty(),
        "El donante eliminado no debe poder recargarse por su identidad");
    assertTrue(
        vacio.getDonantes().stream().noneMatch(d -> d.getId().equals(id)),
        "El donante eliminado no debe aparecer en el listado");
  }

  @Test
  void saveRecargaYEliminaUnaPersonaJuridica() {
    PersonajeJuridica organizacion =
        new PersonajeJuridica(
            new CorreoElectronico("ong@test.com"),
            "30111222333",
            TipoDocumento.CUIT,
            "Fundación Test",
            TipoOrganizacion.ONG,
            "Alimentos");
    organizacion.addMedioDeContacto(new Whatsapp("1145550000"));
    organizacion.agregarRepresentante(new Representante("Maria", "Perez"));
    UUID id = organizacion.getId();

    new DonantesRepository().save(organizacion);
    entityManager().flush();
    entityManager().clear();

    DonantesRepository repositorio = new DonantesRepository();
    Donante porIdentidad = repositorio.findById(id).orElseThrow();

    assertInstanceOf(PersonajeJuridica.class, porIdentidad, "El subtipo jurídico debe conservarse");
    PersonajeJuridica juridica = (PersonajeJuridica) porIdentidad;
    assertEquals("Fundación Test", juridica.getRazonSocial());
    assertEquals(TipoOrganizacion.ONG, juridica.getTipoOrganizacion());
    assertEquals("Alimentos", juridica.getRubro());
    assertEquals("30111222333", juridica.getNumDocumento());
    assertEquals(TipoDocumento.CUIT, juridica.getDocumento());
    assertEquals(
        "ong@test.com",
        juridica.extraerEmail().orElseThrow(),
        "El correo debe recuperarse desde la base");
    assertEquals(
        "ong@test.com",
        juridica.getMedioContactoPredeterminado().getContacto(),
        "El contacto predeterminado debe sobrevivir al viaje");

    var listado = repositorio.getDonantes();
    assertEquals(1, listado.size(), "La organización guardada debe aparecer en el listado");
    Donante porListado =
        listado.stream()
            .filter(d -> d.getId().equals(id))
            .findFirst()
            .orElseThrow(
                () -> new AssertionError("El donante debe aparecer en el listado del repositorio"));
    assertInstanceOf(PersonajeJuridica.class, porListado, "El listado debe conservar el subtipo");
    PersonajeJuridica juridicaListada = (PersonajeJuridica) porListado;
    assertEquals(
        2,
        juridicaListada.getMediosDeContacto().size(),
        "El correo y el WhatsApp deben persistirse");
    assertEquals(
        1,
        juridicaListada.getRepresentantes().size(),
        "El representante debe persistirse con la organización");
    assertEquals("Maria", juridicaListada.getRepresentantes().get(0).getNombre());
    assertEquals("Perez", juridicaListada.getRepresentantes().get(0).getApellido());

    repositorio.deleteById(id);
    entityManager().flush();
    entityManager().clear();

    DonantesRepository vacio = new DonantesRepository();
    assertTrue(
        vacio.findById(id).isEmpty(),
        "La organización eliminada no debe poder recargarse por su identidad");
    assertTrue(
        vacio.getDonantes().stream().noneMatch(d -> d.getId().equals(id)),
        "La organización eliminada no debe aparecer en el listado");
  }

  @Test
  void lasColumnasDeDocumentoGeneroYTipoDeOrganizacionGuardanElNombreDeLaConstanteYNoElIndice() {
    PersonaHumana persona =
        new PersonaHumana(
            new CorreoElectronico("ana@test.com"),
            "Ana",
            "Gomez",
            34,
            "30111222",
            TipoDocumento.DNI,
            Genero.FEMENINO,
            "Av. Siempreviva 742");
    PersonajeJuridica organizacion =
        new PersonajeJuridica(
            new CorreoElectronico("ong@test.com"),
            "30111222333",
            TipoDocumento.CUIT,
            "Fundación Test",
            TipoOrganizacion.ONG,
            "Alimentos");
    UUID idPersona = persona.getId();
    UUID idOrganizacion = organizacion.getId();

    DonantesRepository repositorio = new DonantesRepository();
    repositorio.save(persona);
    repositorio.save(organizacion);
    entityManager().flush();
    entityManager().clear();

    // Recarga con un repositorio nuevo: la lectura sale de la base, no del contexto de
    // persistencia.
    DonantesRepository recarga = new DonantesRepository();
    PersonaHumana personaRecargada =
        assertInstanceOf(PersonaHumana.class, recarga.findById(idPersona).orElseThrow());
    assertEquals(Genero.FEMENINO, personaRecargada.getGenero());
    assertEquals(TipoDocumento.DNI, personaRecargada.getDocumento());
    PersonajeJuridica organizacionRecargada =
        assertInstanceOf(PersonajeJuridica.class, recarga.findById(idOrganizacion).orElseThrow());
    assertEquals(TipoOrganizacion.ONG, organizacionRecargada.getTipoOrganizacion());
    assertEquals(TipoDocumento.CUIT, organizacionRecargada.getDocumento());

    // Consultas nativas: esquivan el mapeo de Hibernate y leen el valor crudo de cada columna. Se
    // restringen por la identidad del donante o de la organización sembrada y no por su documento:
    // la base en memoria es compartida y un número de documento puede repetirse en filas de otras
    // pruebas, mientras que el id señala exactamente la fila que este test persistió.
    Object documentoHumano =
        entityManager()
            .createNativeQuery("SELECT documento FROM Donante WHERE id = :id")
            .setParameter("id", idPersona)
            .getSingleResult();
    Object documentoJuridico =
        entityManager()
            .createNativeQuery("SELECT documento FROM Donante WHERE id = :id")
            .setParameter("id", idOrganizacion)
            .getSingleResult();
    Object genero =
        entityManager()
            .createNativeQuery("SELECT p.genero FROM PersonaHumana p WHERE p.id = :id")
            .setParameter("id", idPersona)
            .getSingleResult();
    Object tipoOrganizacion =
        entityManager()
            .createNativeQuery(
                "SELECT p.tipoOrganizacion FROM PersonajeJuridica p WHERE p.id = :id")
            .setParameter("id", idOrganizacion)
            .getSingleResult();

    assertEquals(
        "DNI", documentoHumano, "La columna debe guardar el nombre del enum, no su índice");
    assertEquals(
        "CUIT", documentoJuridico, "La columna debe guardar el nombre del enum, no su índice");
    assertEquals("FEMENINO", genero, "La columna debe guardar el nombre del enum, no su índice");
    assertEquals(
        "ONG", tipoOrganizacion, "La columna debe guardar el nombre del enum, no su índice");
  }

  @Test
  void getDonantesInactivos_DistingueSegunLaUltimaActividadDelHistorial() {
    SubCategoria subcategoria = new SubCategoria("Sub " + UUID.randomUUID(), Categoria.ALIMENTO);
    PersonaHumana conActividad = crearDonante("111");
    PersonaHumana sinDonaciones = crearDonante("222");
    Donacion donacion =
        nuevaDonacion(
            conActividad,
            List.of(
                new Bien(
                    "Arroz",
                    "",
                    1.0,
                    UnidadMedida.UNIDADES,
                    null,
                    EstadoUso.NO_APLICA,
                    subcategoria)));
    donacion.cambiarEstado(EstadoDonacion.ENTREGADA, null);

    persist(subcategoria);
    persist(conActividad);
    persist(sinDonaciones);
    persist(donacion);

    DonantesRepository repo = new DonantesRepository();
    List<Donante> conActividadReciente = repo.getDonantesInactivos(LocalDate.now());
    List<Donante> conActividadAnterior = repo.getDonantesInactivos(LocalDate.now().plusDays(1));

    assertFalse(conActividadReciente.contains(conActividad));
    assertTrue(conActividadReciente.contains(sinDonaciones));
    assertTrue(conActividadAnterior.contains(conActividad));
  }

  private static PersonaHumana crearDonante(String documento) {
    return new PersonaHumana(
        new CorreoElectronico("donante" + documento + "@test.com"),
        "Ana",
        "Gomez",
        34,
        documento,
        TipoDocumento.DNI,
        Genero.FEMENINO,
        "Av. Siempreviva 742");
  }
}
