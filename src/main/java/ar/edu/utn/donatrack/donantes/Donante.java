package ar.edu.utn.donatrack.donantes;

import ar.edu.utn.donatrack.notificaciones.Notificable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class Donante implements Notificable {
  protected String numDocumento;
  protected TipoDocumento documento;
  protected List<MedioDeContacto> mediosDeContacto = new ArrayList<>();
  protected MedioDeContacto contactoPredeterminado;

  public Donante(MedioDeContacto medioDeContacto, String numDocumento, TipoDocumento documento) {
    Objects.requireNonNull(medioDeContacto);
    this.numDocumento = numDocumento;
    this.documento = documento;
    this.mediosDeContacto.add(medioDeContacto);
    this.contactoPredeterminado = medioDeContacto;
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
