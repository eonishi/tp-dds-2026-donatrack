package ar.edu.utn.donatrack;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.donatrack.donantes.*;
import ar.edu.utn.donatrack.utilidades.ImportadorDonantesCsv;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class RepositoriosTest implements SimplePersistenceTest {
  @Test
  public void agregarDonantesDesdeCsv() throws IOException {
    DonantesRepository repo = new DonantesRepository();
    String path = "src/test/java/ar/edu/utn/donatrack/csv/donantes.csv";

    List<Donante> donantesCsv = ImportadorDonantesCsv.importarDeCsv(path);
    repo.addMany(donantesCsv);

    assertFalse(repo.getDonantes().isEmpty(), "El repo no tendría que estar vacío");
    assertEquals(
        donantesCsv.size(),
        repo.getDonantes().size(),
        "la cantidad de donantes en el repo debe ser igual que el csv");
  }

  @Test
  public void agregarPersonaHumanaYJuridica() {
    DonantesRepository repo = new DonantesRepository();

    // Persona humana
    CorreoElectronico correoElsa = new CorreoElectronico("ElsaPato@hotmail.com");
    PersonaHumana elsa =
        new PersonaHumana(
            correoElsa,
            "Elsa",
            "Pato",
            25,
            "111222333",
            TipoDocumento.DNI,
            Genero.FEMENINO,
            "Calle siempre morida 1234");
    elsa.addMedioDeContacto(new Sms("115558833"));
    elsa.addMedioDeContacto(new Whatsapp("null"));

    // Persona Juridica
    CorreoElectronico emailFundacion = new CorreoElectronico("fundidos@hotmail.com");
    PersonajeJuridica fundacion =
        new PersonajeJuridica(
            emailFundacion,
            "90-111222333-1",
            TipoDocumento.CUIT,
            "Fundacion fundidos",
            TipoOrganizacion.ONG,
            "Lo que venga");
    fundacion.addMedioDeContacto(new Whatsapp("222555588899"));
    fundacion.agregarRepresentante(new Representante("elrre", "presentate"));

    repo.addMany(List.of(elsa, fundacion));

    assertFalse(repo.getDonantes().isEmpty(), "El repo no tendría que estar vacío");
    assertEquals(2, repo.getDonantes().size(), "Deberían haber dos donantes");
    assertTrue(repo.getDonantes().contains(elsa), "Tendría que estar la persona humana");
    assertTrue(repo.getDonantes().contains(fundacion), "Tendría que estar la persona juridica");
    assertEquals(
        3, elsa.getMediosDeContacto().size(), "Elsa Pato debería tener tres medios de contacto");
    assertEquals(
        2, fundacion.getMediosDeContacto().size(), "Fundidos debería tener dos medios de contacto");
    assertEquals(1, fundacion.getRepresentantes().size(), "Fundidos debería tener 1 representante");
  }

  @Test
  public void constructorExigeCorreoElectronico() {
    CorreoElectronico email = new CorreoElectronico("info@fundacion.org");
    PersonajeJuridica pj =
        new PersonajeJuridica(
            email,
            "30-12345678-9",
            TipoDocumento.CUIT,
            "Mi Fundacion",
            TipoOrganizacion.ONG,
            "Salud");

    assertNotNull(pj, "El objeto debe crearse sin errores");
    assertEquals(
        email, pj.getMedioContactoPredeterminado(), "El email debe ser el contactoPredeterminado");
    assertTrue(
        pj.getMediosDeContacto().contains(email),
        "El email debe estar en mediosDeContacto como primer elemento");
    assertEquals(
        "info@fundacion.org",
        pj.extraerEmail().orElse(null),
        "extraerEmail debe devolver el email");
  }

  @Test
  public void upsertActualizaPorEmail() {
    DonantesRepository repo = new DonantesRepository();

    CorreoElectronico email = new CorreoElectronico("existente@mail.com");
    PersonajeJuridica original =
        new PersonajeJuridica(
            email,
            "30-111222333-1",
            TipoDocumento.CUIT,
            "Original SRL",
            TipoOrganizacion.EMPRESA,
            "Comercio");
    repo.addMany(List.of(original));

    CorreoElectronico mismoEmail = new CorreoElectronico("existente@mail.com");
    PersonajeJuridica actualizada =
        new PersonajeJuridica(
            mismoEmail,
            "30-111222333-1",
            TipoDocumento.CUIT,
            "Actualizada SRL",
            TipoOrganizacion.EMPRESA,
            "Industria");
    repo.upsert(List.of(actualizada));

    assertEquals(1, repo.getDonantes().size(), "Debe reemplazar, no duplicar");
    Donante resultado = repo.getDonantes().iterator().next();
    PersonajeJuridica juridicaResultado = (PersonajeJuridica) resultado;
    assertEquals(
        "Actualizada SRL",
        juridicaResultado.getRazonSocial(),
        "La razon social debe ser la actualizada");
  }

  @Test
  public void upsertInsertaSiNoExiste() {
    DonantesRepository repo = new DonantesRepository();

    CorreoElectronico email = new CorreoElectronico("nuevo@mail.com");
    PersonajeJuridica nuevo =
        new PersonajeJuridica(
            email,
            "30-999888777-1",
            TipoDocumento.CUIT,
            "Nueva SRL",
            TipoOrganizacion.EMPRESA,
            "Tecnologia");
    repo.upsert(List.of(nuevo));

    assertEquals(1, repo.getDonantes().size(), "Debe insertar el nuevo donante");
    assertTrue(repo.getDonantes().contains(nuevo), "El donante debe estar en el repo");
  }

  @Test
  public void upsertNoAfectaAddMany() {
    DonantesRepository repo = new DonantesRepository();

    CorreoElectronico emailA = new CorreoElectronico("a@mail.com");
    CorreoElectronico emailB = new CorreoElectronico("b@mail.com");

    PersonajeJuridica a =
        new PersonajeJuridica(
            emailA, "30-111", TipoDocumento.CUIT, "A SRL", TipoOrganizacion.EMPRESA, "Rubro A");
    PersonajeJuridica b =
        new PersonajeJuridica(
            emailB, "30-222", TipoDocumento.CUIT, "B SRL", TipoOrganizacion.EMPRESA, "Rubro B");

    repo.addMany(List.of(a));
    assertEquals(1, repo.getDonantes().size(), "addMany debe haber insertado un donante");

    repo.addMany(List.of(b));
    assertEquals(2, repo.getDonantes().size(), "addMany debe seguir funcionando con otro donante");
  }
}
