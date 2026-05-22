package ar.edu.utn.donatrack;

import ar.edu.utn.donatrack.ServiciosExternosYUtilidades.ImportadorDonantesCSV;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class ImportDonantesCsvTest {

  @Test
  public void importDonantesDesdeCsv() throws IOException {
    // Nose como carajo referenciar al archivo de prueba
    String pathString = "src/test/java/ar/edu/utn/donatrack/csv/donantes.csv";
    Path csvFilePath = Path.of(pathString).toAbsolutePath();
    try(Reader testCsvFile = Files.newBufferedReader(csvFilePath)){
      ImportadorDonantesCSV.importarYRegistrar(testCsvFile);
    }
  }
}
