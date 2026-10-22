package ar.edu.utn.donatrack.beneficiarios;

import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.bienes.UnidadMedida;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class Necesidad {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  private String descripcion;
  private double cantidadRecibida = 0.0;

  @ManyToOne
  @JoinColumn(name = "subcategoria_id", nullable = false)
  private SubCategoria subcategoria;

  private double cantidadObjetivo;

  @Enumerated(EnumType.STRING)
  private UnidadMedida unidad;

  protected Necesidad() {}

  public Necesidad(
      SubCategoria subcategoria, double cantidadObjetivo, UnidadMedida unidad, String descripcion) {
    this.subcategoria = Objects.requireNonNull(subcategoria, "subcategoria no puede ser null");
    if (!Double.isFinite(cantidadObjetivo) || cantidadObjetivo <= 0) {
      throw new IllegalArgumentException("cantidadObjetivo debe ser mayor a cero");
    }
    this.cantidadObjetivo = cantidadObjetivo;
    this.unidad = Objects.requireNonNull(unidad, "unidad no puede ser null");
    this.descripcion = descripcion;
  }

  public Necesidad(
      UUID id,
      SubCategoria subcategoria,
      double cantidadObjetivo,
      UnidadMedida unidad,
      String descripcion) {
    this(subcategoria, cantidadObjetivo, unidad, descripcion);
    this.id = Objects.requireNonNull(id, "id no puede ser null");
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

  /**
   * Indica si un bien satisface esta necesidad por pertenecer a su subcategoría.
   *
   * @param bien bien que se evalúa (no nulo)
   * @return {@code true} si el bien es de la subcategoría de la necesidad
   * @throws NullPointerException si {@code bien} es {@code null}
   */
  public boolean esCompatibleCon(Bien bien) {
    Objects.requireNonNull(bien, "bien no puede ser null");
    return bien.esDeSubcategoria(this.subcategoria);
  }

  /**
   * Indica si un bien se mide en la misma unidad que esta necesidad.
   *
   * @param bien bien que se evalúa (no nulo)
   * @return {@code true} si la unidad del bien coincide con la de la necesidad
   * @throws NullPointerException si {@code bien} es {@code null}
   */
  public boolean tieneMismaUnidadQue(Bien bien) {
    Objects.requireNonNull(bien, "bien no puede ser null");
    return bien.tieneUnidad(this.unidad);
  }

  /**
   * Indica si una cantidad alcanza el objetivo de esta necesidad.
   *
   * @param cantidad cantidad que se compara contra el objetivo
   * @return {@code true} si la cantidad iguala o supera el objetivo
   */
  public boolean alcanzaObjetivoCon(double cantidad) {
    return Double.compare(cantidad, this.cantidadObjetivo) >= 0;
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
