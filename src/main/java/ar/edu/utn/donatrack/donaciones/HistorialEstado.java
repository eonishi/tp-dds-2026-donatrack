package ar.edu.utn.donatrack.donaciones;

import java.time.LocalDate;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class HistorialEstado {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private LocalDate fecha;
  private NombreEstado estadoAlcanzado;
  private String justificacion;

  // Read-only back reference: the foreign key keeps being written by Donacion.historialEstado, so
  // the state machine needs no change. It exists so a history entry can navigate to its donation.
  @ManyToOne
  @JoinColumn(name = "donacion_id", insertable = false, updatable = false)
  private Donacion donacion;

  protected HistorialEstado() {}

  public HistorialEstado(LocalDate fecha, NombreEstado estadoAlcanzado, String justificacion) {
    this.fecha = fecha;
    this.estadoAlcanzado = estadoAlcanzado;
    this.justificacion = justificacion;
  }

  // The generated id orders the history: it is the only monotonic datum, since fecha is a date.
  Long getId() {
    return id;
  }

  public LocalDate getFecha() {
    return fecha;
  }

  public NombreEstado getEstadoAlcanzado() {
    return estadoAlcanzado;
  }

  public String getJustificacion() {
    return justificacion;
  }
}
