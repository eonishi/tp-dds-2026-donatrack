package ar.edu.utn.donatrack.DonantesYContacto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PersonajeJuridica extends Donante {
  private String razonSocial;
  private TipoOrganizacion tipoOrganizacion;
  private String rubro;
  private List<Representante> representantes = new ArrayList<>();

  public PersonajeJuridica(MedioDeContacto medioDeContacto, long numDocumento, TipoDocumento documento, String razonSocial, TipoOrganizacion tipoOrganizacion, String rubro){
    super(medioDeContacto, numDocumento, documento);
    this.razonSocial = razonSocial;
    this.tipoOrganizacion = tipoOrganizacion;
    this.rubro = rubro;
  }

  public void agregarRepresentante(Representante representante){
    if (!representantes.contains(representante)) {
        representantes.add(representante);
    }
  }

  public void actualizarDatosDesdeCSV(PersonajeJuridica nuevosDatos) {
    this.razonSocial = nuevosDatos.getRazonSocial();
    this.numDocumento = nuevosDatos.getNumDocumento();
    this.documento = nuevosDatos.getDocumento();
  }

  public String getRazonSocial() { return razonSocial; }
  public TipoOrganizacion getTipoOrganizacion() { return tipoOrganizacion; }
  public String getRubro() { return rubro; }
  public List<Representante> getRepresentantes() { return Collections.unmodifiableList(representantes); }
}