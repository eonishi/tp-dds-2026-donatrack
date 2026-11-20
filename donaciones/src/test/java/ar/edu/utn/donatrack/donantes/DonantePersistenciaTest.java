package ar.edu.utn.donatrack.donantes;

import static org.junit.jupiter.api.Assertions.*;

import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Prueba de punta a punta de la infraestructura de persistencia: cada donante se guarda a través de
 * la unidad de persistencia, se limpia el entity manager y se recarga por su identidad para
 * comprobar que el estado sobrevivió al viaje por la base.
 */
class DonantePersistenciaTest implements SimplePersistenceTest {

  @Test
  void unDonanteHumanoSobreviveAlViajeDeIdaYVuelta() {
    PersonaHumana donante =
        new PersonaHumana(
            MedioDeContacto.correo("ana@test.com"),
            "Ana",
            "Gomez",
            "30111222",
            TipoDocumento.DNI,
            Genero.FEMENINO,
            "Av. Siempreviva 742");
    donante.addMedioDeContacto(MedioDeContacto.sms("+541145558888"));
    donante.addMedioDeContacto(MedioDeContacto.whatsapp("+541145559999"));
    assertNull(
        donante.getId(),
        "Un donante recién construido no tiene id: lo asigna el proveedor al persistirlo");

    persist(donante);
    UUID id = donante.getId();
    assertNotNull(id, "El proveedor debe asignar el UUID del donante al persistirlo");
    entityManager().flush();
    entityManager().clear();

    Donante recargado = find(Donante.class, id);

    assertNotNull(recargado, "El donante debe poder recargarse por su identidad");
    assertEquals(id, recargado.getId(), "La identidad UUID debe conservarse");
    assertInstanceOf(PersonaHumana.class, recargado, "El subtipo humano debe conservarse");
    PersonaHumana persona = (PersonaHumana) recargado;
    assertEquals("Ana", persona.getNombre());
    assertEquals("Gomez", persona.getApellido());
    assertEquals(Genero.FEMENINO, persona.getGenero());
    assertEquals("30111222", persona.getNumDocumento());
    assertEquals(TipoDocumento.DNI, persona.getDocumento());
    assertEquals(
        "ana@test.com",
        recargado.extraerEmail().orElseThrow(),
        "El correo debe recuperarse desde la base");

    List<MedioDeContacto> medios = recargado.getMediosDeContacto();
    assertEquals(3, medios.size(), "Los tres canales de contacto deben persistirse");
    assertTrue(medios.stream().anyMatch(MedioDeContacto::esCorreo), "Falta el correo");
    assertTrue(medios.stream().anyMatch(m -> m.getCanal() == Canal.SMS), "Falta el SMS");
    assertTrue(medios.stream().anyMatch(m -> m.getCanal() == Canal.WHATSAPP), "Falta el WhatsApp");
    assertEquals(
        "+541145558888",
        medios.stream()
            .filter(m -> m.getCanal() == Canal.SMS)
            .map(MedioDeContacto::getContacto)
            .findFirst()
            .orElseThrow(),
        "El número del SMS debe conservarse");
  }

  @Test
  void unaOrganizacionConRepresentantesSobreviveAlViajeDeIdaYVuelta() {
    PersonajeJuridica organizacion =
        new PersonajeJuridica(
            MedioDeContacto.correo("ong@test.com"),
            "30111222333",
            TipoDocumento.CUIT,
            "Fundación Test",
            TipoOrganizacion.ONG,
            "Alimentos");
    organizacion.agregarRepresentante(new Representante("Ana", "Gomez"));
    assertNull(
        organizacion.getId(),
        "Una organización recién construida no tiene id: lo asigna el proveedor al persistirla");

    persist(organizacion);
    UUID id = organizacion.getId();
    assertNotNull(id, "El proveedor debe asignar el UUID de la organización al persistirla");
    entityManager().flush();
    entityManager().clear();

    Donante recargado = find(Donante.class, id);

    assertNotNull(recargado, "La organización debe poder recargarse por su identidad");
    assertInstanceOf(PersonajeJuridica.class, recargado, "El subtipo jurídico debe conservarse");
    PersonajeJuridica organizacionRecargada = (PersonajeJuridica) recargado;
    assertEquals("Fundación Test", organizacionRecargada.getRazonSocial());
    assertEquals(TipoOrganizacion.ONG, organizacionRecargada.getTipoOrganizacion());
    assertEquals("Alimentos", organizacionRecargada.getRubro());
    assertEquals(
        "ong@test.com",
        organizacionRecargada.extraerEmail().orElseThrow(),
        "El correo de la organización debe recuperarse desde la base");

    List<Representante> representantes = organizacionRecargada.getRepresentantes();
    assertEquals(1, representantes.size(), "El representante debe persistirse");
    assertEquals("Ana", representantes.get(0).getNombre());
    assertEquals("Gomez", representantes.get(0).getApellido());
  }

  @Test
  void laBaseNoAceptaDosDonantesConElMismoDocumento() {
    persist(humana("uno@test.com", "30999888"));
    entityManager().flush();
    persist(humana("otro@test.com", "30999888"));
    var error =
        assertThrows(
            javax.persistence.PersistenceException.class,
            () -> entityManager().flush(),
            "La constraint uk_donante_documento debe rechazar el documento repetido");
    assertTrue(
        DonantesRepository.esDocumentoRepetido(error),
        "El handler de la API tiene que reconocerlo para responder 409");
  }

  @Test
  void otroErrorDeLaBaseNoSeConfundeConUnDocumentoRepetido() {
    assertFalse(
        DonantesRepository.esDocumentoRepetido(
            new javax.persistence.PersistenceException("se cayó la conexión")));
  }

  @Test
  void elDocumentoPropioNoCuentaComoEnUsoAlEditar() {
    PersonaHumana ana = humana("ana.doc@test.com", "30777666");
    persist(ana);
    entityManager().flush();
    DonantesRepository repo = new DonantesRepository();

    assertTrue(repo.documentoEnUso(TipoDocumento.DNI, "30777666", null), "En un alta, está en uso");
    assertFalse(
        repo.documentoEnUso(TipoDocumento.DNI, "30777666", ana.getId()),
        "Editando a Ana, su propio documento está libre");
    assertFalse(repo.documentoEnUso(TipoDocumento.DNI, "11111111", null));
  }

  private static PersonaHumana humana(String correo, String documento) {
    return new PersonaHumana(
        MedioDeContacto.correo(correo),
        "Ana",
        "Gomez",
        documento,
        TipoDocumento.DNI,
        Genero.FEMENINO,
        "Calle 1");
  }
}
