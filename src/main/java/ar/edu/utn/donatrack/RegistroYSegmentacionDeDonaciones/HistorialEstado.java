package ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones;

import java.time.LocalDate;

public class HistorialEstado {
  private LocalDate fecha;
  private NombreEstado estadoAlcanzado;
  private String justificacion;

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