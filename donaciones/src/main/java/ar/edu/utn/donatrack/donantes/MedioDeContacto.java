package ar.edu.utn.donatrack.donantes;

import ar.edu.utn.donatrack.notificaciones.Envio;
import ar.edu.utn.donatrack.notificaciones.NotificadorPorCanal;
import javax.persistence.Column;
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

  @Column(nullable = false)
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

  /**
   * Envía el mensaje a este contacto por el canal que le corresponde.
   *
   * @param mensaje texto a enviar
   * @param canales gateways disponibles para enviar
   * @return el envío realizado
   */
  public abstract Envio enviar(String mensaje, NotificadorPorCanal canales);
}
