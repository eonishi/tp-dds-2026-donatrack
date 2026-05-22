package ar.edu.utn.donatrack.ServiciosExternosYUtilidades;

import ar.edu.utn.donatrack.DonantesYContacto.*;
import com.opencsv.bean.CsvToBeanBuilder;
import java.io.Reader;
import java.util.List;

public class ImportadorDonantesCSV {

  public static List<DonanteCsv> importarDeCsv(Reader reader) {
    return new CsvToBeanBuilder<DonanteCsv>(reader)
        .withType(DonanteCsv.class)
        .withIgnoreLeadingWhiteSpace(true)
        .build()
        .parse();
  }

  public static void importarYRegistrar(Reader reader) {
    List<DonanteCsv> parsed = importarDeCsv(reader);
    for (DonanteCsv csv : parsed) {
      Donante donante = mapearADonante(csv);
      // TODO: Guardar los donantes
    }
  }

  public static Donante mapearADonante(DonanteCsv csv) {
    switch (csv.getTipoPersona()){
      case HUMANA:
        var nombreApellido = csv.getNombreRazonSocial().trim().split(" ");
        var nuevaPersonaHumana =  new PersonaHumana(
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
        var nuevaPersonaJuridica =  new PersonajeJuridica(
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