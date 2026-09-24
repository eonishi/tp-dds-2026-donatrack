package ar.edu.utn.donatrack.bienes;

import java.time.LocalDate;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class Bien {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String descripcion;
  private String foto;
  private double cantidad;

  @Enumerated(EnumType.STRING)
  private UnidadMedida unidad;

  private LocalDate fechaVencimiento;

  @Enumerated(EnumType.STRING)
  private EstadoUso estadoDeUso;

  @ManyToOne
  @JoinColumn(name = "subcategoria_id", nullable = false)
  private SubCategoria subcategoria;

  protected Bien() {}

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
    this.subcategoria = Objects.requireNonNull(subcategoria, "subcategoria no puede ser null");
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
