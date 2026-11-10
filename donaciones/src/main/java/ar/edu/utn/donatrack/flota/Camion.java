package ar.edu.utn.donatrack.flota;

/** Un camión de la flota tal como lo informa logística (getters para Handlebars). */
public final class Camion {
  private final String patente;
  private final double volumenM3;
  private final double alturaM;
  private final double capacidadCargaKg;

  public Camion(String patente, double volumenM3, double alturaM, double capacidadCargaKg) {
    this.patente = patente;
    this.volumenM3 = volumenM3;
    this.alturaM = alturaM;
    this.capacidadCargaKg = capacidadCargaKg;
  }

  public String getPatente() {
    return patente;
  }

  public double getVolumenM3() {
    return volumenM3;
  }

  public double getAlturaM() {
    return alturaM;
  }

  public double getCapacidadCargaKg() {
    return capacidadCargaKg;
  }
}
