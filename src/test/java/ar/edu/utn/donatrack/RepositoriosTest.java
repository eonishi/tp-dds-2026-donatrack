package ar.edu.utn.donatrack;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.donatrack.beneficiarios.BeneficiariosRepository;
import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donantes.*;
import ar.edu.utn.donatrack.utilidades.ImportadorDonantesCsv;
import io.github.flbulgarelli.jpa.extras.test.SimplePersistenceTest;
import java.io.IOException;
import java.util.List;
import java.util.Set;
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

    // Round trip: a fresh repository only sees what actually reached the database.
    entityManager().clear();
    DonantesRepository recargado = new DonantesRepository();
    assertEquals(
        donantesCsv.size(),
        recargado.getDonantes().size(),
        "los donantes importados deben leerse de la base, no de una colección en memoria");
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

    // Round trip: both donors and their cascaded data must come back from the database.
    entityManager().clear();
    Set<Donante> recargados = new DonantesRepository().getDonantes();
    assertEquals(2, recargados.size(), "Los dos donantes deben persistir");
    Donante humanaRecargada = buscarPorDocumento(recargados, "111222333");
    Donante juridicaRecargada = buscarPorDocumento(recargados, "90-111222333-1");
    assertEquals(
        3,
        humanaRecargada.getMediosDeContacto().size(),
        "Elsa Pato debería recuperar sus tres medios de contacto");
    assertEquals(
        2,
        juridicaRecargada.getMediosDeContacto().size(),
        "Fundidos debería recuperar sus dos medios de contacto");
    assertEquals(
        1,
        ((PersonajeJuridica) juridicaRecargada).getRepresentantes().size(),
        "Fundidos debería recuperar su representante");
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

    // Round trip: the update must have reached the database, not only the in-memory instance.
    entityManager().clear();
    Set<Donante> recargados = new DonantesRepository().getDonantes();
    assertEquals(1, recargados.size(), "El upsert no debe duplicar al donante en la base");
    assertEquals(
        "Actualizada SRL",
        ((PersonajeJuridica) recargados.iterator().next()).getRazonSocial(),
        "La razon social actualizada debe recuperarse desde la base");
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

    // Round trip: the inserted donor must be readable from the database by a fresh repository.
    entityManager().clear();
    Set<Donante> recargados = new DonantesRepository().getDonantes();
    assertEquals(1, recargados.size(), "El donante insertado debe persistir");
    assertEquals(
        "nuevo@mail.com",
        recargados.iterator().next().extraerEmail().orElseThrow(),
        "El correo del donante insertado debe recuperarse desde la base");
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

    // Round trip: both batches must persist across two separate addMany calls.
    entityManager().clear();
    Set<Donante> recargados = new DonantesRepository().getDonantes();
    assertEquals(2, recargados.size(), "Los donantes de ambos lotes deben persistir");
    assertEquals(
        "a@mail.com", buscarPorDocumento(recargados, "30-111").extraerEmail().orElseThrow());
    assertEquals(
        "b@mail.com", buscarPorDocumento(recargados, "30-222").extraerEmail().orElseThrow());
  }

  @Test
  public void agregarBeneficiario_DeduplicaPorRazonSocial() {
    BeneficiariosRepository repo = new BeneficiariosRepository();
    EntidadBeneficiaria primera =
        new EntidadBeneficiaria("Hogar Repositorio", "Direccion Original", "1145550001");
    EntidadBeneficiaria duplicada =
        new EntidadBeneficiaria("Hogar Repositorio", "Direccion Nueva", "1145550002");

    repo.addBeneficiario(primera);
    repo.addBeneficiario(duplicada);

    assertEquals(
        1,
        contarPorRazonSocial(repo.getBeneficiarios(), "Hogar Repositorio"),
        "Un beneficiario con la misma razon social no debe duplicarse");

    // Round trip: exactly one entity with that razon social must exist in the database, and it
    // must be the first one registered.
    entityManager().clear();
    Set<EntidadBeneficiaria> recargados = new BeneficiariosRepository().getBeneficiarios();
    assertEquals(
        1,
        contarPorRazonSocial(recargados, "Hogar Repositorio"),
        "El beneficiario debe leerse de la base, sin duplicarse");
    assertEquals(
        "Direccion Original",
        buscarPorRazonSocial(recargados, "Hogar Repositorio").getDireccion(),
        "El beneficiario existente debe conservarse");
  }

  private static long contarPorRazonSocial(
      Set<EntidadBeneficiaria> beneficiarios, String razonSocial) {
    return beneficiarios.stream().filter(e -> razonSocial.equals(e.getRazonSocial())).count();
  }

  private static EntidadBeneficiaria buscarPorRazonSocial(
      Set<EntidadBeneficiaria> beneficiarios, String razonSocial) {
    return beneficiarios.stream()
        .filter(e -> razonSocial.equals(e.getRazonSocial()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No se recargó el beneficiario " + razonSocial));
  }

  private static Donante buscarPorDocumento(Set<Donante> donantes, String numDocumento) {
    return donantes.stream()
        .filter(d -> numDocumento.equals(d.getNumDocumento()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No se recargó el donante " + numDocumento));
  }
}
