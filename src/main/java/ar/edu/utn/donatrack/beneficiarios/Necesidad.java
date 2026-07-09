package ar.edu.utn.donatrack.beneficiarios;

import ar.edu.utn.donatrack.bienes.Bien;

public class Necesidad {
  private String descripcion;
  private double cantidadRecibida = 0.0;
  private final Bien bienObjetivo;

  public Necesidad(Bien bien) {
    this.bienObjetivo = bien;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public void setDescripcion(String descripcion) {
    this.descripcion = descripcion;
  }

  public double getCantidadRecibida() {
    return cantidadRecibida;
  }

  public Bien getBienObjetivo() {
    return bienObjetivo;
  }

  public boolean estaSatisfecha() {
    return cantidadRecibida >= bienObjetivo.getCantidad();
  }

  public void recibirDonacion(double cantidad) {
    if (cantidad > 0) {
      this.cantidadRecibida += cantidad;
    }
  }

  protected void resetearCantidadRecibida() {
    this.cantidadRecibida = 0.0;
  }
}
