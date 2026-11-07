package ar.edu.utn.donatrack.bienes;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Entrada del catálogo de subcategorías compartido entre la oferta ({@code Bien}) y la demanda
 * ({@code Necesidad}).
 *
 * <p>La clave natural es {@code nombre}: dos subcategorías con el mismo nombre son la misma entrada
 * del catálogo. Por eso {@link #equals(Object)} y {@link #hashCode()} se calculan solo sobre {@code
 * nombre} y dejan {@code id} afuera; el id es una identidad sustituta que genera la base con
 * IDENTITY y que muta al persistir, así que incluirlo haría que la igualdad y el hash cambiaran en
 * cuanto la fila se inserta. Esa igualdad por nombre es correcta porque el nombre es único en la
 * base: la tabla lo declara con la restricción {@code uk_subcategoria_nombre}, así que una segunda
 * fila con un nombre ya almacenado se rechaza al insertarse.
 */
@Entity
@Table(
    uniqueConstraints = @UniqueConstraint(name = "uk_subcategoria_nombre", columnNames = "nombre"))
public class SubCategoria {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String nombre;

  @Enumerated(EnumType.STRING)
  private Categoria categoria;

  protected SubCategoria() {}

  public SubCategoria(String nombre, Categoria categoria) {
    this.nombre = nombre;
    this.categoria = categoria;
  }

  public String getNombre() {
    return nombre;
  }

  public Categoria getCategoria() {
    return this.categoria;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SubCategoria that = (SubCategoria) o;
    return java.util.Objects.equals(nombre, that.nombre);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(nombre);
  }
}
