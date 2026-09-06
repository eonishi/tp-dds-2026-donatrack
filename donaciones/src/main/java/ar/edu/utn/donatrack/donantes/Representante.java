package ar.edu.utn.donatrack.donantes;

public class Representante {
  private final String nombre;
  private final String apellido;

  public Representante(String nombre, String apellido) {
    this.nombre = nombre;
    this.apellido = apellido;
  }

  public String getNombre() {
    return nombre;
  }

  public String getApellido() {
    return apellido;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Representante that = (Representante) o;
    return java.util.Objects.equals(nombre, that.nombre)
        && java.util.Objects.equals(apellido, that.apellido);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(nombre, apellido);
  }
}
