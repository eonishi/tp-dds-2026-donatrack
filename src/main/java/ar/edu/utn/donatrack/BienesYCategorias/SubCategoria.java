package ar.edu.utn.donatrack.BienesYCategorias;

public class SubCategoria {
  private String nombre;

  public SubCategoria(String nombre) {
    this.nombre = nombre;
  }

  public String getNombre() { return nombre; }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SubCategoria that = (SubCategoria) o;
    return java.util.Objects.equals(nombre, that.nombre);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(nombre);
  }
}