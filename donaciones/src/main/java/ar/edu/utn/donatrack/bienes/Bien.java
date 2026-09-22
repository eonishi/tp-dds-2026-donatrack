package ar.edu.utn.donatrack.bienes;

import java.time.LocalDate;

public class Bien {
  private final String descripcion;
  private final String foto;
  private final double cantidad;
  private final UnidadMedida unidad;
  private final LocalDate fechaVencimiento;
  private final EstadoUso estadoDeUso;
  private final SubCategoria subcategoria;

  public Bien(
      String descripcion,
      String foto,
      double cantidad,
      UnidadMedida unidad,
      LocalDate fechaVencimiento,
      EstadoUso estadoDeUso,
      SubCategoria subcategoria) {
    this.descripcion = descripcion;
    this.foto = foto;
    this.cantidad = cantidad;
    this.unidad = unidad;
    this.fechaVencimiento = fechaVencimiento;
    this.estadoDeUso = estadoDeUso;
    this.subcategoria = subcategoria;
  }

  public SubCategoria getSubcategoria() {
    return subcategoria;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public String getFoto() {
    return foto;
  }

  public double getCantidad() {
    return cantidad;
  }

  public UnidadMedida getUnidad() {
    return unidad;
  }

  public boolean esPerecedero() {
    return fechaVencimiento != null;
  }

  public LocalDate getFechaVencimiento() {
    return fechaVencimiento;
  }

  public EstadoUso getEstadoDeUso() {
    return estadoDeUso;
  }
}
