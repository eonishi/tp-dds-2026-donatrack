package ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones;

import java.time.LocalDate;

public class HistorialEstado {
  LocalDate fecha = LocalDate.now();
  private NombreEstado estadoAlcanzado;
  private String justificacion;


  public HistorialEstado(LocalDate fecha, NombreEstado estadoAlcanzado, String justificacion) {
    this.fecha = fecha;
    this.estadoAlcanzado = estadoAlcanzado;
    this.justificacion = justificacion;
  }
}
