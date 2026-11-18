package ar.edu.utn.donatrack.importacion;

import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.MedioDeContacto;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.PersonajeJuridica;
import com.opencsv.bean.CsvToBeanBuilder;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ImportadorDonantesCsv {
  private ImportadorDonantesCsv() {
    /* This utility class should not be instantiated */
  }

  public static List<Donante> importarDeCsv(String path) throws IOException {
    Path csvFilePath = Path.of(path).toAbsolutePath();

    try (Reader reader = LectorCsv.abrir(Files.newInputStream(csvFilePath))) {
      List<DonanteCsv> donantesCsv =
          new CsvToBeanBuilder<DonanteCsv>(reader)
              .withType(DonanteCsv.class)
              .withIgnoreLeadingWhiteSpace(true)
              .build()
              .parse();

      return donantesCsv.stream().map(ImportadorDonantesCsv::mapearDonante).toList();
    }
  }

  public static Donante mapearDonante(DonanteCsv csv) {
    switch (csv.getTipoPersona()) {
      case HUMANA:
        var nombreApellido = csv.getNombreRazonSocial().trim().split(" ");
        var nuevaPersonaHumana =
            new PersonaHumana(
                MedioDeContacto.correo(csv.getEmail()),
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
                MedioDeContacto.correo(csv.getEmail()),
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
      donante.addMedioDeContacto(MedioDeContacto.sms(telefono.trim()));
    }
  }
}
