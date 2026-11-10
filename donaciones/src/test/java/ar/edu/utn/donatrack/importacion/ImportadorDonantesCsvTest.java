package ar.edu.utn.donatrack.importacion;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.PersonajeJuridica;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ImportadorDonantesCsvTest {

  @Test
  public void importDonantesDesdeCsv() throws IOException {
    String pathString = "src/test/resources/csv/donantes.csv";

    List<Donante> donantes = ImportadorDonantesCsv.importarDeCsv(pathString);

    assertNotNull(donantes);
    assertFalse(donantes.isEmpty(), "La lista de donantes no debería estar vacía");
    assertEquals(20000, donantes.size());

    // Verificar que parsea correctamente personas humanas
    Donante primerDonante = donantes.get(0);
    assertInstanceOf(PersonaHumana.class, primerDonante);
    PersonaHumana ana = (PersonaHumana) primerDonante;
    assertEquals("Ana", ana.getNombre());
    assertEquals("Navarro", ana.getApellido());
    assertEquals("28456905", ana.getNumDocumento());

    // Verificar que parsea correctamente personas jurídicas
    // La primera jurídica está en la línea 8 del CSV (índice 6 en la lista)
    Donante septimoDonante = donantes.get(6);
    assertInstanceOf(PersonajeJuridica.class, septimoDonante);
  }
}
