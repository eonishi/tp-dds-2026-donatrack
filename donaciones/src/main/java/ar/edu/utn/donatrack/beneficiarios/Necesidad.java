package ar.edu.utn.donatrack.beneficiarios;

import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.bienes.UnidadMedida;

public class Necesidad {
  private String descripcion;
  private double cantidadRecibida = 0.0;
  private final SubCategoria subcategoria;
  private final double cantidadObjetivo;
  private final UnidadMedida unidad;

  public Necesidad(
      SubCategoria subcategoria, double cantidadObjetivo, UnidadMedida unidad, String descripcion) {
    this.subcategoria = subcategoria;
    this.cantidadObjetivo = cantidadObjetivo;
    this.unidad = unidad;
    this.descripcion = descripcion;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public double getCantidadRecibida() {
    return cantidadRecibida;
  }

  public SubCategoria getSubcategoria() {
    return subcategoria;
  }

  public double getCantidadObjetivo() {
    return cantidadObjetivo;
  }

  public UnidadMedida getUnidad() {
    return unidad;
  }

  public boolean estaSatisfecha() {
    return cantidadRecibida >= cantidadObjetivo;
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
