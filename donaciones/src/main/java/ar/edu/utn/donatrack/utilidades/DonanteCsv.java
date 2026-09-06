package ar.edu.utn.donatrack.utilidades;

import ar.edu.utn.donatrack.donantes.TipoDocumento;
import com.opencsv.bean.CsvBindByName;

public class DonanteCsv {
  @CsvBindByName(column = "TipoPersona")
  private String tipoPersona;

  @CsvBindByName(column = "TipoDoc")
  private String tipoDoc;

  @CsvBindByName(column = "Documento")
  private String documento;

  @CsvBindByName(column = "Nombre/Razón Social")
  private String nombreRazonSocial;

  @CsvBindByName(column = "Email")
  private String email;

  @CsvBindByName(column = "Teléfono")
  private String telefono;

  public TipoPersona getTipoPersona() {
    return TipoPersona.valueOf(tipoPersona.trim());
  }

  public TipoDocumento getTipoDoc() {
    return TipoDocumento.valueOf(tipoDoc);
  }

  public String getDocumento() {
    return documento;
  }

  public String getNombreRazonSocial() {
    return nombreRazonSocial;
  }

  public String getEmail() {
    return email;
  }

  public String getTelefono() {
    return telefono;
  }
}
