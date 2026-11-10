package ar.edu.utn.donatrack.notificaciones;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * Un aviso en la bandeja web de una persona donante o de una entidad beneficiaria.
 *
 * <p>Se guarda uno por evento y titular, aunque el aviso salga por varios medios (correo, SMS,
 * WhatsApp): en la web importa qué pasó, no por dónde llegó.
 */
@Entity
@Table(
    name = "aviso_bandeja",
    indexes = {
      @Index(name = "ix_aviso_donante", columnList = "donanteId"),
      @Index(name = "ix_aviso_entidad", columnList = "entidadId")
    })
public class AvisoBandeja {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  private UUID donanteId;
  private Long entidadId;

  @Column(nullable = false, length = 1000)
  private String mensaje;

  @Column(nullable = false)
  private LocalDateTime fecha;

  private boolean leido;

  protected AvisoBandeja() {}

  private AvisoBandeja(UUID donanteId, Long entidadId, String mensaje, LocalDateTime fecha) {
    this.donanteId = donanteId;
    this.entidadId = entidadId;
    this.mensaje = Objects.requireNonNull(mensaje);
    this.fecha = Objects.requireNonNull(fecha);
  }

  public static AvisoBandeja paraDonante(UUID donanteId, String mensaje, LocalDateTime fecha) {
    return new AvisoBandeja(Objects.requireNonNull(donanteId), null, mensaje, fecha);
  }

  public static AvisoBandeja paraEntidad(Long entidadId, String mensaje, LocalDateTime fecha) {
    return new AvisoBandeja(null, Objects.requireNonNull(entidadId), mensaje, fecha);
  }

  public UUID getId() {
    return id;
  }

  public UUID getDonanteId() {
    return donanteId;
  }

  public Long getEntidadId() {
    return entidadId;
  }

  public String getMensaje() {
    return mensaje;
  }

  public LocalDateTime getFecha() {
    return fecha;
  }

  public boolean isLeido() {
    return leido;
  }

  public void marcarLeido() {
    this.leido = true;
  }
}
