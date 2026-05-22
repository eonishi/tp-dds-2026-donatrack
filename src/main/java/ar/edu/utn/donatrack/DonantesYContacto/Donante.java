package ar.edu.utn.donatrack.DonantesYContacto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class Donante {
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

  public void setContactoPredeterminado(MedioDeContacto medioDeContacto){
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

  public String getNumDocumento() { return numDocumento; }
  public TipoDocumento getDocumento() { return documento; }

  public Optional<String> extraerEmail() {
    return mediosDeContacto.stream()
        .filter(m -> m instanceof CorreoElectronico)
        .map(MedioDeContacto::getContacto)
        .findFirst();
  }
}