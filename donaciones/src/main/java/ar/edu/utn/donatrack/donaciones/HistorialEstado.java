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

  private LocalDateTime fecha;

  // Obligatorio a propósito: toda entrada del historial registra la transición que la produjo.
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private EstadoDonacion estadoAlcanzado;

  private String justificacion;

  protected HistorialEstado() {}

  /**
   * Construye la entrada del historial que registra la transición que se produjo.
   *
   * <p>Una entrada guarda solo el instante, el estado alcanzado y la justificación: la posición que
   * ocupa en el historial y el estado del que proviene se derivan en el borde de la API a partir
   * del orden de la colección.
   *
   * @param fecha instante en que se produjo la transición
   * @param estadoAlcanzado estado al que transicionó la donación
   * @param justificacion motivo de la transición, o {@code null} si no requiere justificación
   */
  public HistorialEstado(
      LocalDateTime fecha, EstadoDonacion estadoAlcanzado, String justificacion) {
    this.fecha = fecha;
    this.estadoAlcanzado = estadoAlcanzado;
    this.justificacion = justificacion;
  }

  public LocalDateTime getFecha() {
    return fecha;
  }

  public EstadoDonacion getEstadoAlcanzado() {
    return estadoAlcanzado;
  }

  public String getJustificacion() {
    return justificacion;
  }
}
