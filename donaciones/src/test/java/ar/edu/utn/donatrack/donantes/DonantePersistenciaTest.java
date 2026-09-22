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

    persist(donante);
    entityManager().flush();
    entityManager().clear();

    Donante recargado = find(Donante.class, id);

    assertNotNull(recargado, "El donante debe poder recargarse por su identidad");
    assertEquals(id, recargado.getId(), "La identidad UUID debe conservarse");
    assertInstanceOf(PersonaHumana.class, recargado, "El subtipo humano debe conservarse");
    PersonaHumana persona = (PersonaHumana) recargado;
    assertEquals("Ana", persona.getNombre());
    assertEquals("Gomez", persona.getApellido());
    assertEquals(34, persona.getEdad());
    assertEquals(Genero.FEMENINO, persona.getGenero());
    assertEquals("30111222", persona.getNumDocumento());
    assertEquals(TipoDocumento.DNI, persona.getDocumento());
    assertEquals(
        "ana@test.com",
        recargado.extraerEmail().orElseThrow(),
        "El correo debe recuperarse desde la base");

    List<MedioDeContacto> medios = recargado.getMediosDeContacto();
    assertEquals(3, medios.size(), "Los tres canales de contacto deben persistirse");
    assertTrue(medios.stream().anyMatch(CorreoElectronico.class::isInstance), "Falta el correo");
    assertTrue(medios.stream().anyMatch(Sms.class::isInstance), "Falta el SMS");
    assertTrue(medios.stream().anyMatch(Whatsapp.class::isInstance), "Falta el WhatsApp");
    assertEquals(
        "1145558888",
        medios.stream()
            .filter(Sms.class::isInstance)
            .map(MedioDeContacto::getContacto)
            .findFirst()
            .orElseThrow(),
        "El número del SMS debe conservarse");
  }

  @Test
  void unaOrganizacionConRepresentantesSobreviveAlViajeDeIdaYVuelta() {
    PersonajeJuridica organizacion =
        new PersonajeJuridica(
            new CorreoElectronico("ong@test.com"),
            "30111222333",
            TipoDocumento.CUIT,
            "Fundación Test",
            TipoOrganizacion.ONG,
            "Alimentos");
    organizacion.agregarRepresentante(new Representante("Ana", "Gomez"));
    UUID id = organizacion.getId();

    persist(organizacion);
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
}
