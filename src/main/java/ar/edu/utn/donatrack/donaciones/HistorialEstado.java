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

  // Referencia inversa de solo lectura: la clave foránea la sigue escribiendo
  // Donacion.historialEstado, así que la máquina de estados no necesita cambios. Existe para que
  // una entrada del historial pueda navegar a su donación.
  @ManyToOne
  @JoinColumn(name = "donacion_id", insertable = false, updatable = false)
  private Donacion donacion;

  protected HistorialEstado() {}

  public HistorialEstado(LocalDate fecha, NombreEstado estadoAlcanzado, String justificacion) {
    this.fecha = fecha;
    this.estadoAlcanzado = estadoAlcanzado;
    this.justificacion = justificacion;
  }

  // El id generado ordena el historial: es el único dato monotónico, ya que fecha es una fecha.
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
