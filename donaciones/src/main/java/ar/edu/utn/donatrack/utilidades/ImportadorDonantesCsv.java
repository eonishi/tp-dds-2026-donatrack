package ar.edu.utn.donatrack.utilidades;

import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.PersonajeJuridica;
import ar.edu.utn.donatrack.donantes.Sms;
import com.opencsv.bean.CsvToBeanBuilder;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ImportadorDonantesCsv {
  private ImportadorDonantesCsv() {
    /* This utility class should not be instantiated */
  }

  public static List<Donante> importarDeCsv(String path) throws IOException {
    Path csvFilePath = Path.of(path).toAbsolutePath();

    try (Reader reader = bomSafeReader(csvFilePath)) {
      List<DonanteCsv> donantesCsv =
          new CsvToBeanBuilder<DonanteCsv>(reader)
              .withType(DonanteCsv.class)
              .withIgnoreLeadingWhiteSpace(true)
              .build()
              .parse();

      return donantesCsv.stream().map(ImportadorDonantesCsv::mapearDonante).toList();
    }
  }

  /**
   * Abre un Reader que detecta y descarta el BOM UTF-8 (EF BB BF) si está presente, sin necesidad
   * de dependencias externas.
   *
   * <p>El {@link BufferedInputStream} subyacente no se cierra acá a propósito: el caller {@link
   * #importarDeCsv} lo cierra mediante su {@code try-with-resources} cuando cierra el {@link
   * Reader} retornado.
   */
  private static Reader bomSafeReader(Path path) throws IOException {
    var bis = new BufferedInputStream(Files.newInputStream(path));
    bis.mark(3);
    byte[] header = new byte[3];
    if (bis.read(header) != 3
        || header[0] != (byte) 0xEF
        || header[1] != (byte) 0xBB
        || header[2] != (byte) 0xBF) {
      bis.reset(); // No hay BOM, rebobinar al inicio
    }
    // Si hay BOM, los 3 bytes ya fueron consumidos del stream
    return new BufferedReader(new InputStreamReader(bis, StandardCharsets.UTF_8));
  }

  public static Donante mapearDonante(DonanteCsv csv) {
    switch (csv.getTipoPersona()) {
      case HUMANA:
        var nombreApellido = csv.getNombreRazonSocial().trim().split(" ");
        var nuevaPersonaHumana =
            new PersonaHumana(
                new CorreoElectronico(csv.getEmail()),
                nombreApellido[0],
                (nombreApellido.length > 1) ? nombreApellido[1] : "",
                csv.getDocumento(),
                csv.getTipoDoc(),
                null,
                null);
        agregarTelefono(nuevaPersonaHumana, csv.getTelefono());
        return nuevaPersonaHumana;

      case JURIDICA:
        var nuevaPersonaJuridica =
            new PersonajeJuridica(
                new CorreoElectronico(csv.getEmail()),
                csv.getDocumento(),
                csv.getTipoDoc(),
                csv.getNombreRazonSocial(),
                null,
                null);
        agregarTelefono(nuevaPersonaJuridica, csv.getTelefono());
        return nuevaPersonaJuridica;

      default:
        throw new IllegalArgumentException("TipoPersona importada no es válida");
    }
  }

  // El teléfono es opcional: muchas filas reales no lo traen.
  private static void agregarTelefono(Donante donante, String telefono) {
    if (telefono != null && !telefono.isBlank()) {
      donante.addMedioDeContacto(new Sms(telefono.trim()));
    }
  }
}
