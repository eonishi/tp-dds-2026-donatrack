package ar.edu.utn.donatrack.donantes;

import ar.edu.utn.donatrack.notificaciones.Notificable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Donante implements Notificable {
  @Id private UUID id;
  protected String numDocumento;
  protected TipoDocumento documento;

  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "donante_id")
  protected List<MedioDeContacto> mediosDeContacto = new ArrayList<>();

  @ManyToOne
  @JoinColumn(name = "contacto_predeterminado_id")
  protected MedioDeContacto contactoPredeterminado;

  protected Donante() {}

  public Donante(MedioDeContacto medioDeContacto, String numDocumento, TipoDocumento documento) {
    this(UUID.randomUUID(), medioDeContacto, numDocumento, documento);
  }

  protected Donante(
      UUID id, MedioDeContacto medioDeContacto, String numDocumento, TipoDocumento documento) {
    this.id = Objects.requireNonNull(id, "id no puede ser null");
    Objects.requireNonNull(medioDeContacto);
    this.numDocumento = numDocumento;
    this.documento = documento;
    this.mediosDeContacto.add(medioDeContacto);
    this.contactoPredeterminado = medioDeContacto;
    // Aca agregaria el enviar el mail de bienvenida
  }

  public UUID getId() {
    return id;
  }

  public void addMedioDeContacto(MedioDeContacto medioDeContacto) {
    if (medioDeContacto != null && !mediosDeContacto.contains(medioDeContacto)) {
      this.mediosDeContacto.add(medioDeContacto);
    }
  }

  public void setContactoPredeterminado(MedioDeContacto medioDeContacto) {
    if (medioDeContacto != null && mediosDeContacto.contains(medioDeContacto)) {
      this.contactoPredeterminado = medioDeContacto;
    }
  }

  public MedioDeContacto getMedioContactoPredeterminado() {
    return this.contactoPredeterminado;
  }

  public List<MedioDeContacto> getMediosDeContacto() {
    return Collections.unmodifiableList(mediosDeContacto);
  }

  public String getNumDocumento() {
    return numDocumento;
  }

  public TipoDocumento getDocumento() {
    return documento;
  }

  public Optional<String> extraerEmail() {
    return mediosDeContacto.stream()
        .filter(CorreoElectronico.class::isInstance)
        .map(MedioDeContacto::getContacto)
        .findFirst();
  }

  public void actualizarIdentificacion(String numDocumento, TipoDocumento documento) {
    this.numDocumento = numDocumento;
    this.documento = documento;
  }

  public void reemplazarMediosDeContacto(
      List<MedioDeContacto> nuevosMedios, int indicePredeterminado) {
    if (nuevosMedios == null || nuevosMedios.isEmpty()) {
      throw new IllegalArgumentException("Debe informar al menos un medio de contacto.");
    }
    if (indicePredeterminado < 0 || indicePredeterminado >= nuevosMedios.size()) {
      throw new IllegalArgumentException("El contacto predeterminado no existe.");
    }
    this.mediosDeContacto = new ArrayList<>(nuevosMedios);
    this.contactoPredeterminado = this.mediosDeContacto.get(indicePredeterminado);
  }

  void merge(Donante entrante) {
    this.numDocumento = entrante.numDocumento;
    this.documento = entrante.documento;
    for (MedioDeContacto medio : entrante.mediosDeContacto) {
      addMedioDeContacto(medio);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Donante donante)) {
      return false;
    }
    return Objects.equals(numDocumento, donante.numDocumento) && documento == donante.documento;
  }

  @Override
  public int hashCode() {
    return Objects.hash(numDocumento, documento);
  }
}
