package ar.edu.utn.donatrack.donantes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PersonajeJuridica extends Donante {
  private String razonSocial;
  private TipoOrganizacion tipoOrganizacion;
  private String rubro;
  private final List<Representante> representantes = new ArrayList<>();

  public PersonajeJuridica(
      CorreoElectronico email,
      String numDocumento,
      TipoDocumento documento,
      String razonSocial,
      TipoOrganizacion tipoOrganizacion,
      String rubro) {
    this(
        UUID.randomUUID(),
        email,
        numDocumento,
        documento,
        razonSocial,
        tipoOrganizacion,
        rubro);
  }

  public PersonajeJuridica(
      UUID id,
      CorreoElectronico email,
      String numDocumento,
      TipoDocumento documento,
      String razonSocial,
      TipoOrganizacion tipoOrganizacion,
      String rubro) {
    super(id, email, numDocumento, documento);
    this.razonSocial = razonSocial;
    this.tipoOrganizacion = tipoOrganizacion;
    this.rubro = rubro;
  }

  public void agregarRepresentante(Representante representante) {
    if (!representantes.contains(representante)) {
      representantes.add(representante);
    }
  }

  public String getRazonSocial() {
    return razonSocial;
  }

  public TipoOrganizacion getTipoOrganizacion() {
    return tipoOrganizacion;
  }

  public String getRubro() {
    return rubro;
  }

  public List<Representante> getRepresentantes() {
    return Collections.unmodifiableList(representantes);
  }

  public void actualizarDatosJuridicos(
      String razonSocial,
      TipoOrganizacion tipoOrganizacion,
      String rubro,
      List<Representante> representantes) {
    this.razonSocial = razonSocial;
    this.tipoOrganizacion = tipoOrganizacion;
    this.rubro = rubro;
    this.representantes.clear();
    if (representantes != null) {
      representantes.forEach(this::agregarRepresentante);
    }
  }

  @Override
  void merge(Donante entrante) {
    super.merge(entrante);
    if (entrante instanceof PersonajeJuridica pj) {
      this.razonSocial = pj.razonSocial;
    }
  }
}
