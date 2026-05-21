package ar.edu.utn.donatrack.BeneficiariosYNecesidades;

public class Necesidad {
  private long id;
  private String descripcion;
  private double cantidadObjetivo;
  private double cantidadRecibida;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public void setDescripcion(String descripcion) {
    this.descripcion = descripcion;
  }

  public double getCantidadObjetivo() {
    return cantidadObjetivo;
  }

  public void setCantidadObjetivo(double cantidadObjetivo) {
    this.cantidadObjetivo = cantidadObjetivo;
  }

  public double getCantidadRecibida() {
    return cantidadRecibida;
  }

  public void setCantidadRecibida(double cantidadRecibida) {
    this.cantidadRecibida = cantidadRecibida;
  }

  public boolean estaSatisfecha(){
    return cantidadRecibida >= cantidadObjetivo;
  }

  public void recibirDonacion(Double cantidad){
    this.cantidadRecibida += cantidad;
  }
}
