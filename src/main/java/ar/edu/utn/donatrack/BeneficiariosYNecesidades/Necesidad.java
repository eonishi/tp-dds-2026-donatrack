package ar.edu.utn.donatrack.BeneficiariosYNecesidades;

public class Necesidad {
  private final String descripcion;
  private long id;
  private double cantidadObjetivo;
  private double cantidadRecibida;

  public Necesidad(String descripcion, double cantidadObjetivo) {
    this.descripcion = descripcion;
    this.cantidadObjetivo = cantidadObjetivo;
  }

  public void setCantidadRecibida(double cantidadRecibida) {
    this.cantidadRecibida = cantidadRecibida;
  }

  public boolean estaSatisfecha() {
    return this.cantidadRecibida >= this.cantidadObjetivo;
  }

  public void recibirDonacion(Double cantidad) {
    this.cantidadObjetivo += cantidad;
  }
}
