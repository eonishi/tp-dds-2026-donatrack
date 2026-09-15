package ar.edu.utn.donatrack.beneficiarios;

import ar.edu.utn.donatrack.donaciones.Donacion;
import java.time.LocalDate;
import java.util.Objects;

public final class Recepcion {
  private final Donacion donacion;
  private final LocalDate fechaRecepcion;

  public Recepcion(Donacion donacion, LocalDate fechaRecepcion) {
    this.donacion = Objects.requireNonNull(donacion, "donacion no puede ser null");
    this.fechaRecepcion =
        Objects.requireNonNull(fechaRecepcion, "fechaRecepcion no puede ser null");
  }

  public Donacion getDonacion() {
    return donacion;
  }

  public LocalDate getFechaRecepcion() {
    return fechaRecepcion;
  }
}
