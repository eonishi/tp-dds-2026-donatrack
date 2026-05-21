package ar.edu.utn.donatrack.ServiciosExternosYUtilidades;
import com.opencsv.bean.CsvToBeanBuilder;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ImportadorDonantesCSV {

  public static void main(String[] args) {

    // Poné la ruta exacta donde guardaste tu archivo
    String rutaArchivo = "donantes_import_20000_UTF8_BOM.csv";

    try {
      // Usamos Files de Java 11+ asegurando que lea en formato UTF-8 (para los acentos)
      Reader reader = Files.newBufferedReader(Path.of(rutaArchivo), StandardCharsets.UTF_8);

      // Magia de OpenCSV: Convertir el CSV en Lista de Objetos
      List<DonanteCsv> listaDonantes = new CsvToBeanBuilder<DonanteCsv>(reader)
          .withType(DonanteCsv.class)
          .withIgnoreLeadingWhiteSpace(true) // Limpia espacios en blanco accidentales
          .build()
          .parse();

      System.out.println("¡Éxito! Se importaron " + listaDonantes.size() + " donantes.");

      // Imprimimos los primeros 3 para comprobar que funcionó bien
      System.out.println("\n--- MUESTRA DE LOS PRIMEROS 3 DONANTES ---");
      for (int i = 0; i < 3; i++) {
        DonanteCsv d = listaDonantes.get(i);
        System.out.println(d.getNombreRazonSocial() + " | DNI/CUIT: " + d.getDocumento() + " | Email: " + d.getEmail());
      }

      // IMPORTANTE: Cerrar el archivo para liberar memoria
      reader.close();

    } catch (Exception e) {
      System.out.println("Ocurrió un error al procesar el archivo: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
