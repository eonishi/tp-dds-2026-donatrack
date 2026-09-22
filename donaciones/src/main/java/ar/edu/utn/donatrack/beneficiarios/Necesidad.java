package ar.edu.utn.donatrack.beneficiarios;

import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.bienes.UnidadMedida;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class Necesidad {
  @Id private UUID id;

  private String descripcion;
  private double cantidadRecibida = 0.0;

  @ManyToOne
  @JoinColumn(name = "subcategoria_id")
  private SubCategoria subcategoria;

  private double cantidadObjetivo;
  private UnidadMedida unidad;

  protected Necesidad() {}

  public Necesidad(
      SubCategoria subcategoria, double cantidadObjetivo, UnidadMedida unidad, String descripcion) {
    this(UUID.randomUUID(), subcategoria, cantidadObjetivo, unidad, descripcion);
  }

  public Necesidad(
      UUID id,
      SubCategoria subcategoria,
      double cantidadObjetivo,
      UnidadMedida unidad,
      String descripcion) {
    this.id = Objects.requireNonNull(id, "id no puede ser null");
    this.subcategoria = Objects.requireNonNull(subcategoria, "subcategoria no puede ser null");
    if (!Double.isFinite(cantidadObjetivo) || cantidadObjetivo <= 0) {
      throw new IllegalArgumentException("cantidadObjetivo debe ser mayor a cero");
    }
    this.cantidadObjetivo = cantidadObjetivo;
    this.unidad = Objects.requireNonNull(unidad, "unidad no puede ser null");
    this.descripcion = descripcion;
  }

  public UUID getId() {
    return id;
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

  public void actualizar(
      SubCategoria subcategoria, double cantidadObjetivo, UnidadMedida unidad, String descripcion) {
    if (!Double.isFinite(cantidadObjetivo) || cantidadObjetivo <= 0) {
      throw new IllegalArgumentException("cantidadObjetivo debe ser mayor a cero");
    }
    this.subcategoria = Objects.requireNonNull(subcategoria, "subcategoria no puede ser null");
    this.cantidadObjetivo = cantidadObjetivo;
    this.unidad = Objects.requireNonNull(unidad, "unidad no puede ser null");
    this.descripcion = descripcion;
  }

  protected void resetearCantidadRecibida() {
    this.cantidadRecibida = 0.0;
  }
}
