package ar.edu.utn.donatrack.DonantesYContacto;


import java.util.ArrayList;
import java.util.List;
public class PersonajeJuridica extends Donante{
  private String razonSocial;
  private TipoOrganizacion tipoOrganizacion;
  private String rubro;
  private List<Representante> representantes = new ArrayList<>();


  public PersonajeJuridica(MedioDeContacto medioDeContacto,int numDocumento, TipoDocumento documento,String razonSocial, TipoOrganizacion tipoOrganizacion, String rubro){
    super(medioDeContacto,numDocumento,documento);
    this.razonSocial = razonSocial;
    this.tipoOrganizacion = tipoOrganizacion;
    this.rubro = rubro;
  }

  public void agregarRepresentante (Representante representante){
    representantes.add(representante);
  }

}
