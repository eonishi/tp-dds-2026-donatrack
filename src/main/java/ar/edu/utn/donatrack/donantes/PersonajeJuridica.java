package ar.edu.utn.donatrack.donantes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

@Entity
public class PersonajeJuridica extends Donante {
  private String razonSocial;
  private TipoOrganizacion tipoOrganizacion;
  private String rubro;

  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "donante_id")
  private List<Representante> representantes = new ArrayList<>();

  protected PersonajeJuridica() {}

  public PersonajeJuridica(
      CorreoElectronico email,
      String numDocumento,
      TipoDocumento documento,
      String razonSocial,
      TipoOrganizacion tipoOrganizacion,
      String rubro) {
    super(email, numDocumento, documento);
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

  @Override
  void merge(Donante entrante) {
    super.merge(entrante);
    if (entrante instanceof PersonajeJuridica pj) {
      this.razonSocial = pj.razonSocial;
    }
  }
}
