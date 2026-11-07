package ar.edu.utn.donatrack.bienes;

public enum Categoria {
  ALIMENTO,
  MOBILIARIO,
  VESTIMENTA;

  public boolean requiereEstadoDeUso() {
    return this == MOBILIARIO || this == VESTIMENTA;
  }
}
