package ar.edu.utn.donatrack.DonantesYContacto;

public abstract class MedioDeContacto {
  private final String contacto;
  public MedioDeContacto(String contacto) { this.contacto = contacto; }
  public String getContacto() { return contacto; }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MedioDeContacto that = (MedioDeContacto) o;
    return java.util.Objects.equals(contacto, that.contacto);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(contacto);
  }

  public abstract void enviarMensaje (String mensaje);
}