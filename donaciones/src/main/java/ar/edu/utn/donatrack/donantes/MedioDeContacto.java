package ar.edu.utn.donatrack.donantes;

import ar.edu.utn.donatrack.notificaciones.NotificadorPorCanal;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.SequenceGenerator;

// SINGLE_TABLE y no JOINED: las subclases no agregan columnas propias (salvo la entidad dueña de
// un correo), y con JOINED cada medio eran dos INSERT en tablas que se alternan, lo que corta los
// lotes de Hibernate en lotes de una sola fila.
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class MedioDeContacto {
  // SEQUENCE y no IDENTITY: con IDENTITY la base inventa el id recién al insertar, así que
  // Hibernate tiene que mandar cada INSERT apenas se persiste y no puede agruparlos en lotes.
  // Con la secuencia reserva 50 ids por viaje y los asigna en memoria.
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "medio_de_contacto_seq")
  @SequenceGenerator(
      name = "medio_de_contacto_seq",
      sequenceName = "medio_de_contacto_seq",
      allocationSize = 50)
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
   */
  public abstract void enviar(String mensaje, NotificadorPorCanal canales);
}
