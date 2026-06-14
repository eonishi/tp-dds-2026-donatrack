package ar.edu.utn.donatrack.donantes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PersonajeJuridica extends Donante {
  private final String razonSocial;
  private final TipoOrganizacion tipoOrganizacion;
  private final String rubro;
  private final List<Representante> representantes = new ArrayList<>();

  public PersonajeJuridica(
      MedioDeContacto medioDeContacto,
      String numDocumento,
      TipoDocumento documento,
      String razonSocial,
      TipoOrganizacion tipoOrganizacion,
      String rubro) {
    super(medioDeContacto, numDocumento, documento);
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
}
