package ar.edu.utn.donatrack.donaciones;

import java.time.LocalDateTime;

public class HistorialEstado {
  private final long secuencia;
  private final LocalDateTime fecha;
  private final EstadoDonacion estadoAnterior;
  private final EstadoDonacion estadoAlcanzado;
  private final String justificacion;

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
