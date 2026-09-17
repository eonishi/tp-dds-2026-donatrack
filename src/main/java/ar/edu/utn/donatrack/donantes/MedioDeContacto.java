package ar.edu.utn.donatrack.donantes;

import ar.edu.utn.donatrack.notificaciones.Notificador;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class MedioDeContacto {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  protected MedioDeContacto() {}

  private String contacto;

  public MedioDeContacto(String contacto) {
    this.contacto = contacto;
  }

  public String getContacto() {
    return contacto;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MedioDeContacto that = (MedioDeContacto) o;
    return java.util.Objects.equals(contacto, that.contacto);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(contacto);
  }

  public abstract void aceptar(Notificador notificador, String mensaje);
}
