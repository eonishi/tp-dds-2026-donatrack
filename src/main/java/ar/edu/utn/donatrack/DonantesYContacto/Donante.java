package ar.edu.utn.donatrack.DonantesYContacto;

import java.util.ArrayList;
import java.util.List;

public class Donante {
  public MedioDeContacto medioDeContacto; // kept for backwards compatibility
  protected long numDocumento;
  protected TipoDocumento documento;
  protected List<MedioDeContacto> mediosDeContacto = new ArrayList<>();
  protected MedioDeContacto medioContactoPredeterminado;

  //Constructor
  public Donante(MedioDeContacto medioDeContacto, long numDocumento, TipoDocumento documento) {
    this.medioDeContacto = medioDeContacto;
    this.numDocumento = numDocumento;
    this.documento = documento;
    if (medioDeContacto != null) {
      this.mediosDeContacto.add(medioDeContacto);
      this.medioContactoPredeterminado = medioDeContacto;
    }
  }

  //Métodos
  public void setMedioDeContacto(MedioDeContacto medioDeContacto) {
    this.medioDeContacto = medioDeContacto;
    if (medioDeContacto != null && !mediosDeContacto.contains(medioDeContacto)) {
      mediosDeContacto.add(medioDeContacto);
    }
  }

  public void setMedioContactoPredeterminado(MedioDeContacto medioDeContacto){
    this.medioContactoPredeterminado = medioDeContacto;
    this.medioDeContacto = medioDeContacto;
    if (medioDeContacto != null && !mediosDeContacto.contains(medioDeContacto)) {
      mediosDeContacto.add(medioDeContacto);
    }
  }

  // Typo support for old test setMedioContactoPredterminado
  public void setMedioContactoPredterminado(MedioDeContacto medioDeContacto){
    setMedioContactoPredeterminado(medioDeContacto);
  }

  public MedioDeContacto getMedioDeContacto() {
    return medioDeContacto;
  }

  public MedioDeContacto getMedioContactoPredeterminado() {
    return medioContactoPredeterminado != null ? medioContactoPredeterminado : medioDeContacto;
  }

  public List<MedioDeContacto> getMediosDeContacto() {
    return mediosDeContacto;
  }

  public void agregarMedioDeContacto(MedioDeContacto medio) {
    if (medio != null && !mediosDeContacto.contains(medio)) {
      mediosDeContacto.add(medio);
    }
  }

  public String getEmail() {
    for (MedioDeContacto medio : mediosDeContacto) {
      if (medio instanceof CorreoElectronico) {
        return medio.getContacto();
      }
    }
    return null;
  }

  public long getNumDocumento() {
    return numDocumento;
  }

  public TipoDocumento getDocumento() {
    return documento;
  }

  public void setNumDocumento(long numDocumento) {
    this.numDocumento = numDocumento;
  }

  public void setDocumento(TipoDocumento documento) {
    this.documento = documento;
  }
}
