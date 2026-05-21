package ar.edu.utn.donatrack.DonantesYContacto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public abstract class Donante {
  protected long numDocumento;
  protected TipoDocumento documento;
  protected List<MedioDeContacto> mediosDeContacto = new ArrayList<>();
  protected MedioDeContacto medioContactoPredeterminado;

  public Donante(MedioDeContacto medioDeContacto, long numDocumento, TipoDocumento documento) {  
    this.numDocumento = numDocumento;
    this.documento = documento;
    if (medioDeContacto != null) {
      this.mediosDeContacto.add(medioDeContacto);
    }
  }

  public void setMedioDeContacto(MedioDeContacto medioDeContacto) {
    if (medioDeContacto != null && !mediosDeContacto.contains(medioDeContacto)) {
      this.mediosDeContacto.add(medioDeContacto);    
    }
  }

  public void setMedioContactoPredeterminado(MedioDeContacto medioDeContacto){
    if (medioDeContacto != null && !mediosDeContacto.contains(medioDeContacto)) {
      this.medioContactoPredeterminado = medioDeContacto;
      this.mediosDeContacto.add(medioDeContacto);
    }
  }

  public MedioDeContacto getMedioContactoPredeterminado() {
    return medioContactoPredeterminado != null ? medioContactoPredeterminado : mediosDeContacto.get(0);
  }

  public List<MedioDeContacto> getMediosDeContacto() {
    return Collections.unmodifiableList(mediosDeContacto);
  }

  public long getNumDocumento() { return numDocumento; }
  public TipoDocumento getDocumento() { return documento; }

  public Optional<String> extraerEmail() {
    return mediosDeContacto.stream()
        .filter(m -> m instanceof CorreoElectronico)
        .map(MedioDeContacto::getContacto)
        .findFirst();
  }
}