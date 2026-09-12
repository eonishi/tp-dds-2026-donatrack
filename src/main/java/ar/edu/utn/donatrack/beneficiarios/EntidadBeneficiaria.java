package ar.edu.utn.donatrack.beneficiarios;

import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.MedioDeContacto;
import ar.edu.utn.donatrack.notificaciones.Notificable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.CascadeType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity 
public class EntidadBeneficiaria implements Notificable {
  @Id 
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String razonSocial;
  private String direccion;
  private String telefono;

  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "entidad_id")
  private List<Necesidad> necesidadesExtraordinarias = new ArrayList<>();
  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "entidad_id")
  private List<NecesidadRecurrente> necesidadesRecurrentes = new ArrayList<>();
  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "entidad_id")
  private List<CorreoElectronico> correos = new ArrayList<>();
  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "entidad_id")
  private List<RecepcionDonacion> recepciones = new ArrayList<>();

  protected EntidadBeneficiaria() {}

  public EntidadBeneficiaria(String razonSocial, String direccion, String telefono) {
    this.razonSocial = razonSocial;
    this.direccion = direccion;
    this.telefono = telefono;
  }

  public String getRazonSocial() {
    return razonSocial;
  }

  public String getDireccion() {
    return direccion;
  }

  public String getTelefono() {
    return telefono;
  }

  public List<Necesidad> getNecesidadesExtraordinarias() {
    return Collections.unmodifiableList(necesidadesExtraordinarias);
  }

  public List<NecesidadRecurrente> getNecesidadesRecurrentes() {
    return Collections.unmodifiableList(necesidadesRecurrentes);
  }

  public List<CorreoElectronico> getCorreos() {
    return Collections.unmodifiableList(correos);
  }

  @Override
  public List<MedioDeContacto> getMediosDeContacto() {
    return Collections.unmodifiableList(new ArrayList<>(correos));
  }

  public List<RecepcionDonacion> getRecepciones() {
    return Collections.unmodifiableList(recepciones);
  }

  public void agregarNecesidadExtraordinaria(Necesidad n) {
    this.necesidadesExtraordinarias.add(n);
  }

  public void agregarNecesidadRecurrente(NecesidadRecurrente n) {
    this.necesidadesRecurrentes.add(n);
  }

  public void agregarCorreo(CorreoElectronico correo) {
    this.correos.add(correo);
  }

  public void registrarRecepcion(Donacion donacion, LocalDate fechaRecepcion) {
    this.recepciones.add(new RecepcionDonacion(donacion, fechaRecepcion));
    this.actualizarNecesidadesCon(donacion);
  }


  private void actualizarNecesidadesCon(Donacion donacion) {
    necesidadesExtraordinarias.forEach(necesidad -> aplicarSiCoincide(necesidad, donacion));
    necesidadesRecurrentes.forEach(necesidad -> aplicarSiCoincide(necesidad, donacion));
  }

  private void aplicarSiCoincide(Necesidad necesidad, Donacion donacion) {
    if (necesidad.getSubcategoria().equals(donacion.getSubcategoria())) {
      necesidad.recibirDonacion(donacion.getCantidadTotal());
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EntidadBeneficiaria that)) {
      return false;
    }
    return Objects.equals(razonSocial, that.razonSocial);
  }

  @Override
  public int hashCode() {
    return Objects.hash(razonSocial);
  }
}
