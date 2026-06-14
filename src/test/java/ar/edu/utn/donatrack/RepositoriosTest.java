package ar.edu.utn.donatrack;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.donatrack.donantes.*;
import ar.edu.utn.donatrack.utilidades.ImportadorDonantesCsv;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class RepositoriosTest {
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
    Whatsapp wasaEmpresa = new Whatsapp("222555588899");
    PersonajeJuridica fundacion =
        new PersonajeJuridica(
            wasaEmpresa,
            "90-111222333-1",
            TipoDocumento.CUIT,
            "Fundacion fundidos",
            TipoOrganizacion.ONG,
            "Lo que venga");
    fundacion.addMedioDeContacto(new CorreoElectronico("fundidos@hotmail.com"));
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
}
