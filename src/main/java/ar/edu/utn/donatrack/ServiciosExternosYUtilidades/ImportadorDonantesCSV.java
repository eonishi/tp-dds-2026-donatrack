package ar.edu.utn.donatrack.ServiciosExternosYUtilidades;

import ar.edu.utn.donatrack.DonantesYContacto.*;
import com.opencsv.bean.CsvToBeanBuilder;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
    String emailStr = (csv.getEmail() != null && !csv.getEmail().trim().isEmpty()) ? csv.getEmail().trim() : "sin-email@donatrack.org";
    CorreoElectronico email = new CorreoElectronico(emailStr);

    java.util.List<MedioDeContacto> opcionales = new java.util.ArrayList<>();
    if (csv.getTelefono() != null && !csv.getTelefono().trim().isEmpty()) {
      opcionales.add(new Whatsapp(csv.getTelefono().trim()));
    }

    TipoDocumento tipoDoc = TipoDocumento.DNI;
    if (csv.getTipoDoc() != null) {
      String t = csv.getTipoDoc().toUpperCase().trim();
      try {
        tipoDoc = TipoDocumento.valueOf(t);
      } catch (IllegalArgumentException e) {
        if (t.equals("CUIL") || t.equals("CUIT")) {
          tipoDoc = TipoDocumento.CUIT;
        } else if (t.equals("PASAPORTE")) {
          tipoDoc = TipoDocumento.PASAPORTE;
        } else if (t.equals("LC")) {
          tipoDoc = TipoDocumento.LC;
        } else if (t.equals("LE")) {
          tipoDoc = TipoDocumento.LE;
        }
      }
    }

    long numDoc = 0;
    if (csv.getDocumento() != null) {
      try {
        String docClean = csv.getDocumento().replaceAll("[^0-9]", "");
        if (!docClean.isEmpty()) {
          numDoc = Long.parseLong(docClean);
        }
      } catch (NumberFormatException e) {
        // keep 0
      }
    }

    if ("JURIDICA".equalsIgnoreCase(csv.getTipoPersona()) || "JURÍDICA".equalsIgnoreCase(csv.getTipoPersona())) {
      MedioDeContacto medio = email;
      if (emailStr.startsWith("sin-email") && !opcionales.isEmpty()) {
        medio = opcionales.get(0);
      }
      PersonajeJuridica juridica = new PersonajeJuridica(
          medio,
          numDoc,
          tipoDoc,
          csv.getNombreRazonSocial(),
          TipoOrganizacion.ONG,
          "Rubro no especificado"
      );
      if (!opcionales.isEmpty() && opcionales.get(0) != medio) {
        juridica.agregarMedioDeContacto(opcionales.get(0));
      }
      return juridica;
    } else {
      String nombre = csv.getNombreRazonSocial();
      String apellido = "";
      if (nombre != null && nombre.contains(" ")) {
        int idx = nombre.indexOf(" ");
        apellido = nombre.substring(idx + 1);
        nombre = nombre.substring(0, idx);
      }
      PersonaHumana humana = new PersonaHumana(
          email,
          opcionales,
          email,
          nombre,
          apellido,
          30,
          numDoc,
          tipoDoc,
          "No especifica",
          "Direccion no especificada"
      );
      return humana;
    }
  }

  public static void main(String[] args) {
    // Poné la ruta exacta donde guardaste tu archivo
    String rutaArchivo = "donantes_import_20000_UTF8_BOM.csv";

    try {
      // Usamos Files de Java 11+ asegurando que lea en formato UTF-8 (para los acentos)
      Reader reader = Files.newBufferedReader(Path.of(rutaArchivo), StandardCharsets.UTF_8);

      // Magia de OpenCSV: Convertir el CSV en Lista de Objetos
      List<DonanteCsv> listaDonantes = importarDeCsv(reader);

      System.out.println("¡Éxito! Se importaron " + listaDonantes.size() + " donantes.");

      // Imprimimos los primeros 3 para comprobar que funcionó bien
      System.out.println("\n--- MUESTRA DE LOS PRIMEROS 3 DONANTES ---");
      int muestra = Math.min(3, listaDonantes.size());
      for (int i = 0; i < muestra; i++) {
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
