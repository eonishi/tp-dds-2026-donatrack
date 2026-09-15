package ar.edu.utn.donatrack.donaciones;

import java.time.LocalDate;

public class HistorialEstado {
  private final long secuencia;
  private final LocalDate fecha;
  private final NombreEstado estadoAnterior;
  private final NombreEstado estadoAlcanzado;
  private final String justificacion;

  public HistorialEstado(LocalDate fecha, NombreEstado estadoAlcanzado, String justificacion) {
    this(0, fecha, null, estadoAlcanzado, justificacion);
  }

  private HistorialEstado(
      long secuencia,
      LocalDate fecha,
      NombreEstado estadoAnterior,
      NombreEstado estadoAlcanzado,
      String justificacion) {
    this.secuencia = secuencia;
    this.fecha = fecha;
    this.estadoAnterior = estadoAnterior;
    this.estadoAlcanzado = estadoAlcanzado;
    this.justificacion = justificacion;
  }

  public HistorialEstado contextualizar(long secuencia, NombreEstado estadoAnterior) {
    return new HistorialEstado(
        secuencia, fecha, estadoAnterior, estadoAlcanzado, justificacion);
  }

  public long getSecuencia() {
    return secuencia;
  }

  public LocalDate getFecha() {
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
