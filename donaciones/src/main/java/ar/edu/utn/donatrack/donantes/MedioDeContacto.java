package ar.edu.utn.donatrack.donantes;

import ar.edu.utn.donatrack.notificaciones.NotificadorPorCanal;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

/**
 * Value object: un canal y el contacto en ese canal. No tiene identidad propia; dos medios con el
 * mismo canal y contacto son el mismo medio. Se guarda embebido en la tabla de su dueño.
 *
 * <p>El comportamiento por canal vive en {@link Canal} y no en subclases porque Hibernate 5 no
 * soporta herencia en los {@code @Embeddable}: al leer siempre arma esta clase, y el canal viaja
 * como un campo más.
 */
@Embeddable
public class MedioDeContacto {
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Canal canal;

  @Column(nullable = false)
  private String contacto;

  protected MedioDeContacto() {}

  public MedioDeContacto(Canal canal, String contacto) {
    this.canal = Objects.requireNonNull(canal);
    this.contacto = contacto;
  }

  public static MedioDeContacto correo(String direccion) {
    return new MedioDeContacto(Canal.CORREO, direccion);
  }

  public static MedioDeContacto sms(String numero) {
    return new MedioDeContacto(Canal.SMS, numero);
  }

  public static MedioDeContacto whatsapp(String numero) {
    return new MedioDeContacto(Canal.WHATSAPP, numero);
  }

  /** Para los lugares que exigen un correo (el alta de un donante, los correos de una entidad). */
  public static MedioDeContacto exigirCorreo(MedioDeContacto medio) {
    if (medio == null || !medio.esCorreo()) {
      throw new IllegalArgumentException("Se esperaba un correo electrónico.");
    }
    return medio;
  }

  public Canal getCanal() {
    return canal;
  }

  public String getContacto() {
    return contacto;
  }

  public boolean esCorreo() {
    return canal == Canal.CORREO;
  }

  public boolean esTelefono() {
    return canal == Canal.SMS || canal == Canal.WHATSAPP;
  }

  /**
   * Envía el mensaje a este contacto por el canal que le corresponde.
   *
   * @param mensaje texto a enviar
   * @param canales gateways disponibles para enviar
   */
  public void enviar(String mensaje, NotificadorPorCanal canales) {
    canal.enviar(contacto, mensaje, canales);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MedioDeContacto that)) {
      return false;
    }
    return canal == that.canal && Objects.equals(contacto, that.contacto);
  }

  @Override
  public int hashCode() {
    return Objects.hash(canal, contacto);
  }

  @Override
  public String toString() {
    return canal + ":" + contacto;
  }
}
