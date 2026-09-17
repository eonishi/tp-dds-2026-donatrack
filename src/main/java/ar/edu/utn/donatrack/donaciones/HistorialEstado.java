package ar.edu.utn.donatrack.donaciones;

import java.time.LocalDate;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class HistorialEstado {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private LocalDate fecha;
  private NombreEstado estadoAlcanzado;
  private String justificacion;

  protected HistorialEstado() {}

  public HistorialEstado(LocalDate fecha, NombreEstado estadoAlcanzado, String justificacion) {
    this.fecha = fecha;
    this.estadoAlcanzado = estadoAlcanzado;
    this.justificacion = justificacion;
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
