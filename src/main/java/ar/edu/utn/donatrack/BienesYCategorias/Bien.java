package ar.edu.utn.donatrack.BienesYCategorias;

import java.time.LocalDate;

public class Bien {
  private String descripcion;
  private String foto; // probablmente url
  private double cantidad;
  private UnidadMedida unidad;
  private boolean esPerecedero;
  LocalDate fechaVencimiento;
  private EstadoUso estadoDeUso;
  private SubCategoria subcategoria;

  public Bien(String descripcion, String foto, double cantidad, UnidadMedida unidad, boolean esPerecedero, LocalDate fechaVencimiento, EstadoUso estadoDeUso, SubCategoria subcategoria) {
    this.descripcion = descripcion;
    this.foto = foto;
    this.cantidad = cantidad;
    this.unidad = unidad;
    this.esPerecedero = esPerecedero;
    this.fechaVencimiento = fechaVencimiento;
    this.estadoDeUso = estadoDeUso;
    this.subcategoria = subcategoria;
  }
}
