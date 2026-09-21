package ar.edu.utn.donatrack.donaciones;

import java.time.LocalDateTime;

public class HistorialEstado {
  private final long secuencia;
  private final LocalDateTime fecha;
  private final NombreEstado estadoAnterior;
  private final NombreEstado estadoAlcanzado;
  private final String justificacion;

  public HistorialEstado(LocalDateTime fecha, NombreEstado estadoAlcanzado, String justificacion) {
    this(0, fecha, null, estadoAlcanzado, justificacion);
  }

  private HistorialEstado(
      long secuencia,
      LocalDateTime fecha,
      NombreEstado estadoAnterior,
      NombreEstado estadoAlcanzado,
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
  public HistorialEstado contextualizar(long secuencia, NombreEstado estadoAnterior) {
    return new HistorialEstado(secuencia, fecha, estadoAnterior, estadoAlcanzado, justificacion);
  }

  public long getSecuencia() {
    return secuencia;
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
