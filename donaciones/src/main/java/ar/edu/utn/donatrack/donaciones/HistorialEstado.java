package ar.edu.utn.donatrack.donaciones;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class HistorialEstado {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private long secuencia;
  private LocalDateTime fecha;

  @Enumerated(EnumType.STRING)
  private EstadoDonacion estadoAnterior;

  // Obligatorio a propósito: toda entrada del historial registra la transición que la produjo.
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private EstadoDonacion estadoAlcanzado;

  private String justificacion;

  protected HistorialEstado() {}

  public HistorialEstado(
      LocalDateTime fecha, EstadoDonacion estadoAlcanzado, String justificacion) {
    this(0, fecha, null, estadoAlcanzado, justificacion);
  }

  private HistorialEstado(
      long secuencia,
      LocalDateTime fecha,
      EstadoDonacion estadoAnterior,
      EstadoDonacion estadoAlcanzado,
      String justificacion) {
    this.secuencia = secuencia;
    this.fecha = fecha;
    this.estadoAnterior = estadoAnterior;
    this.estadoAlcanzado = estadoAlcanzado;
    this.justificacion = justificacion;
  }

  /**
   * Devuelve una copia de este evento ubicada en la secuencia indicada.
   *
   * @param secuencia posición del evento dentro del historial de la donación
   * @param estadoAnterior estado desde el que se produjo la transición
   * @return un nuevo evento con la secuencia y el estado anterior indicados
   */
  public HistorialEstado contextualizar(long secuencia, EstadoDonacion estadoAnterior) {
    return new HistorialEstado(secuencia, fecha, estadoAnterior, estadoAlcanzado, justificacion);
  }

  public long getSecuencia() {
    return secuencia;
  }

  public LocalDateTime getFecha() {
    return fecha;
  }

  public EstadoDonacion getEstadoAlcanzado() {
    return estadoAlcanzado;
  }

  public EstadoDonacion getEstadoAnterior() {
    return estadoAnterior;
  }

  public String getJustificacion() {
    return justificacion;
  }
}
