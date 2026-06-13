package ar.edu.utn.donatrack.BeneficiariosYNecesidades;

import ar.edu.utn.donatrack.BienesYCategorias.Bien;

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

  public boolean estaSatisfecha() {
    return cantidadRecibida >= bienObjetivo.getCantidad();
  }

  public void recibirDonacion(Double cantidad) {
    if (cantidad != null && cantidad > 0) {
      this.cantidadRecibida += cantidad;
    }
  }

  protected void resetearCantidadRecibida() {
    this.cantidadRecibida = 0.0;
  }
}