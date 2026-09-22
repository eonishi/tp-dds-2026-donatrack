package ar.edu.utn.donatrack.bienes;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class SubCategoria {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String nombre;
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
