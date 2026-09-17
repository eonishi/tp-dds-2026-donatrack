package ar.edu.utn.donatrack.logistica;

import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * Representa un camión en el sistema de gestión de flota.
 *
 * <p>Entidad inmutable identificada por su patente. Cada camión tiene dimensiones físicas (volumen,
 * altura) y capacidad de carga necesarias para planificar rutas y asignar donaciones.
 */
@Entity
public class Camion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  protected Camion() {}

  private String patente;
  private double volumenM3;
  private double alturaM;
  private double capacidadCargaKg;

  /**
   * Crea un nuevo camión con los atributos especificados.
   *
   * @param patente identificador único del camión (no nulo)
   * @param volumenM3 volumen en metros cúbicos (no negativo, no NaN)
   * @param alturaM altura en metros (no negativo, no NaN)
   * @param capacidadCargaKg capacidad de carga en kilogramos (no negativo, no NaN)
   * @throws NullPointerException si {@code patente} es {@code null}
   * @throws IllegalArgumentException si alguno de los valores numéricos es negativo o NaN
   */
  public Camion(String patente, double volumenM3, double alturaM, double capacidadCargaKg) {
    this.patente = Objects.requireNonNull(patente, "patente no debe ser nula");

    if (volumenM3 < 0 || Double.isNaN(volumenM3)) {
      throw new IllegalArgumentException("volumenM3 no debe ser negativo ni NaN: " + volumenM3);
    }
    if (alturaM < 0 || Double.isNaN(alturaM)) {
      throw new IllegalArgumentException("alturaM no debe ser negativo ni NaN: " + alturaM);
    }
    if (capacidadCargaKg < 0 || Double.isNaN(capacidadCargaKg)) {
      throw new IllegalArgumentException(
          "capacidadCargaKg no debe ser negativo ni NaN: " + capacidadCargaKg);
    }

    this.volumenM3 = volumenM3;
    this.alturaM = alturaM;
    this.capacidadCargaKg = capacidadCargaKg;
  }

  /**
   * @return patente del camión
   */
  public String getPatente() {
    return patente;
  }

  /**
   * @return volumen en metros cúbicos
   */
  public double getVolumenM3() {
    return volumenM3;
  }

  /**
   * @return altura en metros
   */
  public double getAlturaM() {
    return alturaM;
  }

  /**
   * @return capacidad de carga en kilogramos
   */
  public double getCapacidadCargaKg() {
    return capacidadCargaKg;
  }

  // ── Identity based on patente ──────────────────────────────────────

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Camion that = (Camion) o;
    return patente.equals(that.patente);
  }

  @Override
  public int hashCode() {
    return patente.hashCode();
  }

  @Override
  public String toString() {
    return "Camion{patente='" + patente + "'}";
  }
}
