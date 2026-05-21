package ar.edu.utn.donatrack.DonantesYContacto;


import java.util.ArrayList;
import java.util.List;
public class PersonajeJuridica {
  private static String razonSocial;
  private TipoOrganizacion tipoOrganizacion;
  private static String rubro;
  private List<Representante> representantes = new ArrayList<>();


  public PersonajeJuridica(String razonSocial, TipoOrganizacion tipoOrganizacion, String rubro){
    this.razonSocial = razonSocial;
    this.tipoOrganizacion = tipoOrganizacion;
    this.rubro = rubro;
  }

  public void agregarRepresentante (Representante representante){
    representantes.add(representante);
  }

}
