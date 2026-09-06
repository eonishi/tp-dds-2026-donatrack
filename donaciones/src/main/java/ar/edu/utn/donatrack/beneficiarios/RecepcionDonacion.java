package ar.edu.utn.donatrack.beneficiarios;

import ar.edu.utn.donatrack.donaciones.Donacion;
import java.time.LocalDate;

public class RecepcionDonacion {
  private final Donacion donacion;
  private final LocalDate fechaRecepcion;

  public RecepcionDonacion(Donacion donacion, LocalDate fechaRecepcion) {
    this.donacion = donacion;
    this.fechaRecepcion = fechaRecepcion;
  }

  public Donacion getDonacion() {
    return donacion;
  }

  public LocalDate getFechaRecepcion() {
    return fechaRecepcion;
  }
}
