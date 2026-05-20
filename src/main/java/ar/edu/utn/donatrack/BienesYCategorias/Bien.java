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
}
