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

  public static void importarYRegistrar(Reader reader, DonantesRepository repo) {
    List<DonanteCsv> parsed = importarDeCsv(reader);
    for (DonanteCsv csv : parsed) {
      Donante donante = mapearADonante(csv);
      repo.registrarOActualizar(donante);
    }
  }

  public static Donante mapearADonante(DonanteCsv csv) {
    String emailStr = (csv.getEmail() != null && !csv.getEmail().trim().isEmpty()) ? csv.getEmail().trim() : "sin-email";
    CorreoElectronico email = new CorreoElectronico(emailStr);

    TipoDocumento tipoDoc = TipoDocumento.DNI; 
    if (csv.getTipoDoc() != null) {
      try {
        tipoDoc = TipoDocumento.valueOf(csv.getTipoDoc().toUpperCase().trim());
      } catch (IllegalArgumentException e) {
        if (csv.getTipoDoc().trim().equalsIgnoreCase("CUIL") || csv.getTipoDoc().trim().equalsIgnoreCase("CUIT")) {
          tipoDoc = TipoDocumento.CUIT;
        }
      }
    }

    long numDoc = 0;
    if (csv.getDocumento() != null) {
      String docClean = csv.getDocumento().replaceAll("[^0-9]", "");
      if (!docClean.isEmpty()) {
        try {
          numDoc = Long.parseLong(docClean);
        } catch (NumberFormatException ignored) {}
      }
    }

    if ("JURIDICA".equalsIgnoreCase(csv.getTipoPersona()) || "JURÍDICA".equalsIgnoreCase(csv.getTipoPersona())) {
      PersonajeJuridica juridica = new PersonajeJuridica(
          email, 
          numDoc, 
          tipoDoc, 
          csv.getNombreRazonSocial() != null ? csv.getNombreRazonSocial() : "", 
          TipoOrganizacion.ONG, 
          ""
      );
      if (csv.getTelefono() != null && !csv.getTelefono().trim().isEmpty()) {
        juridica.setMedioDeContacto(new Whatsapp(csv.getTelefono().trim()));
      }
      return juridica;
    } else {
      String nombreCompleto = csv.getNombreRazonSocial() != null ? csv.getNombreRazonSocial() : "";
      String nombre = nombreCompleto;
      String apellido = "";
      if (nombreCompleto.contains(" ")) {
        int idx = nombreCompleto.indexOf(" ");
        apellido = nombreCompleto.substring(idx + 1);
        nombre = nombreCompleto.substring(0, idx);
      }
      
      PersonaHumana humana = new PersonaHumana(
          email, nombre, apellido, 0, numDoc, tipoDoc, "", ""
      );
      if (csv.getTelefono() != null && !csv.getTelefono().trim().isEmpty()) {
        humana.setMedioDeContacto(new Whatsapp(csv.getTelefono().trim()));
      }
      return humana;
    }
  }
}