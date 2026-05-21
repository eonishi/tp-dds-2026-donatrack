package ar.edu.utn.donatrack.BeneficiariosYNecesidades;

public abstract class Necesidad {
  private String descripcion;
  private double cantidadObjetivo;
  private double cantidadRecibida = 0.0;

  public String getDescripcion() { return descripcion; }
  public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

  public double getCantidadObjetivo() { return cantidadObjetivo; }
  public void setCantidadObjetivo(double cantidadObjetivo) { this.cantidadObjetivo = cantidadObjetivo; }

  public double getCantidadRecibida() { return cantidadRecibida; }

  public boolean estaSatisfecha(){
    return cantidadRecibida >= cantidadObjetivo;
  }

  public void recibirDonacion(Double cantidad){
    if (cantidad != null && cantidad > 0) {
        this.cantidadRecibida += cantidad;
    }
  }

  protected void resetearCantidadRecibida() {
      this.cantidadRecibida = 0.0;
  }
}