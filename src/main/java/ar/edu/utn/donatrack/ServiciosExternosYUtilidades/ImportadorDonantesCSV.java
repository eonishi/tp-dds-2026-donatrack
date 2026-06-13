package ar.edu.utn.donatrack.ServiciosExternosYUtilidades;

import ar.edu.utn.donatrack.DonantesYContacto.*;
import com.opencsv.bean.CsvToBeanBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ImportadorDonantesCSV {

  public static List<Donante> importarDeCsv(String path) throws IOException {
    Path csvFilePath = Path.of(path).toAbsolutePath();

    try (Reader reader = bomSafeReader(csvFilePath)) {
      List<DonanteCsv> donantesCsv = new CsvToBeanBuilder<DonanteCsv>(reader)
          .withType(DonanteCsv.class)
          .withIgnoreLeadingWhiteSpace(true)
          .build()
          .parse();

      return donantesCsv.stream()
          .map(ImportadorDonantesCSV::mapearADonante)
          .toList();
    }
  }

  /**
   * Abre un Reader que detecta y descarta el BOM UTF-8 (EF BB BF)
   * si está presente, sin necesidad de dependencias externas.
   */
  private static Reader bomSafeReader(Path path) throws IOException {
    var bis = new BufferedInputStream(Files.newInputStream(path));
    bis.mark(3);
    byte[] header = new byte[3];
    if (bis.read(header) == 3
        && header[0] == (byte) 0xEF
        && header[1] == (byte) 0xBB
        && header[2] == (byte) 0xBF) {
      // BOM detectado, ya fue consumido del stream
    } else {
      bis.reset(); // No hay BOM, rebobinar
    }
    return new BufferedReader(new InputStreamReader(bis, StandardCharsets.UTF_8));
  }

  public static Donante mapearADonante(DonanteCsv csv) {
    switch (csv.getTipoPersona()) {
      case HUMANA:
        var nombreApellido = csv.getNombreRazonSocial().trim().split(" ");
        var nuevaPersonaHumana = new PersonaHumana(
            new CorreoElectronico(csv.getEmail()),
            nombreApellido[0],
            (nombreApellido.length > 1) ? nombreApellido[1] : "",
            null,
            csv.getDocumento(),
            csv.getTipoDoc(),
            null,
            null
        );
        nuevaPersonaHumana.addMedioDeContacto(new SMS(csv.getTelefono()));
        return nuevaPersonaHumana;

      case JURIDICA:
        var nuevaPersonaJuridica = new PersonajeJuridica(
            new CorreoElectronico(csv.getEmail()),
            csv.getDocumento(),
            csv.getTipoDoc(),
            csv.getNombreRazonSocial(),
            null,
            null
        );
        nuevaPersonaJuridica.addMedioDeContacto(new SMS(csv.getTelefono()));
        return nuevaPersonaJuridica;

      default:
        throw new IllegalArgumentException("TipoPersona importada no es válida");
    }
  }
}