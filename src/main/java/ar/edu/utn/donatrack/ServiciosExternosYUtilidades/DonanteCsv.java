package ar.edu.utn.donatrack.ServiciosExternosYUtilidades;

import com.opencsv.bean.CsvBindByName;

public class DonanteCsv {

  // El nombre 'column' tiene que ser EXACTAMENTE igual a la primera fila de tu Excel/CSV
  @CsvBindByName(column = "TipoPersona")
  private String tipoPersona;

  @CsvBindByName(column = "TipoDoc")
  private String tipoDoc;

  @CsvBindByName(column = "Documento")
  private String documento; // Lo dejo como String porque a veces los CUIT tienen guiones

  @CsvBindByName(column = "Nombre/Razón Social")
  private String nombreRazonSocial;

  @CsvBindByName(column = "Email")
  private String email;

  @CsvBindByName(column = "Teléfono")
  private String telefono;

  // --- CONSTRUCTOR VACÍO (Obligatorio para que la librería funcione) ---
  public DonanteCsv() {
  }

  // --- GETTERS Y SETTERS (Obligatorios para leer/escribir los datos) ---
  public String getTipoPersona() { return tipoPersona; }
  public void setTipoPersona(String tipoPersona) { this.tipoPersona = tipoPersona; }

  public String getTipoDoc() { return tipoDoc; }
  public void setTipoDoc(String tipoDoc) { this.tipoDoc = tipoDoc; }

  public String getDocumento() { return documento; }
  public void setDocumento(String documento) { this.documento = documento; }

  public String getNombreRazonSocial() { return nombreRazonSocial; }
  public void setNombreRazonSocial(String nombreRazonSocial) { this.nombreRazonSocial = nombreRazonSocial; }

  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }

  public String getTelefono() { return telefono; }
  public void setTelefono(String telefono) { this.telefono = telefono; }
}