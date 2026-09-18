package ar.edu.utn.donatrack.donaciones;

import java.time.LocalDate;
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

  private LocalDate fecha;

  // Obligatorio a propósito: toda entrada del historial registra la transición que la produjo.
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private EstadoDonacion estadoAlcanzado;

  private String justificacion;

  protected HistorialEstado() {}

  public HistorialEstado(LocalDate fecha, EstadoDonacion estadoAlcanzado, String justificacion) {
    this.fecha = fecha;
    this.estadoAlcanzado = estadoAlcanzado;
    this.justificacion = justificacion;
  }

  public LocalDate getFecha() {
    return fecha;
  }

  public EstadoDonacion getEstadoAlcanzado() {
    return estadoAlcanzado;
  }

  public String getJustificacion() {
    return justificacion;
  }
}
