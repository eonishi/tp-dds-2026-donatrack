package ar.edu.utn.donatrack.donaciones;

import java.time.LocalDateTime;

public class HistorialEstado {
  private LocalDateTime fecha;
  private NombreEstado estadoAlcanzado;
  private String justificacion;

  public HistorialEstado(LocalDateTime fecha, NombreEstado estadoAlcanzado, String justificacion) {
    this.fecha = fecha;
    this.estadoAnterior = estadoAnterior;
    this.estadoAlcanzado = estadoAlcanzado;
    this.justificacion = justificacion;
  }

  public LocalDateTime getFecha() {
    return fecha;
  }

  public NombreEstado getEstadoAlcanzado() {
    return estadoAlcanzado;
  }

  public NombreEstado getEstadoAnterior() {
    return estadoAnterior;
  }

  public String getJustificacion() {
    return justificacion;
  }
}
