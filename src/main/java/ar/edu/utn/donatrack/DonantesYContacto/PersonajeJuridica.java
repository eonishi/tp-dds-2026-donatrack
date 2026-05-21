package ar.edu.utn.donatrack.DonantesYContacto;


import java.util.ArrayList;
import java.util.List;
public class PersonajeJuridica extends Donante{
  private String razonSocial;
  private TipoOrganizacion tipoOrganizacion;
  private String rubro;
  private List<Representante> representantes = new ArrayList<>();


  public PersonajeJuridica(MedioDeContacto medioDeContacto,long numDocumento, TipoDocumento documento,String razonSocial, TipoOrganizacion tipoOrganizacion, String rubro){
    super(medioDeContacto,numDocumento,documento);
    this.razonSocial = razonSocial;
    this.tipoOrganizacion = tipoOrganizacion;
    this.rubro = rubro;
  }

  public void agregarRepresentante (Representante representante){
    representantes.add(representante);
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
    return representantes;
  }

  public void setRazonSocial(String razonSocial) {
    this.razonSocial = razonSocial;
  }

  public void setTipoOrganizacion(TipoOrganizacion tipoOrganizacion) {
    this.tipoOrganizacion = tipoOrganizacion;
  }

  public void setRubro(String rubro) {
    this.rubro = rubro;
  }
}
