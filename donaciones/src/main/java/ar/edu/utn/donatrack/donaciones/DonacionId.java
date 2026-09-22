package ar.edu.utn.donatrack.donaciones;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifica una donación de forma única, también al cruzar al microservicio de logística.
 *
 * <p>El contrato de integración fija {@code idDonacion} como UUID: el volumen de donaciones impide
 * coordinar una secuencia entre servicios, a diferencia de las entidades beneficiarias.
 *
 * @param value valor del identificador (no nulo)
 */
public record DonacionId(UUID value) {
  public DonacionId {
    Objects.requireNonNull(value, "value no debe ser null");
  }

  /**
   * Genera un identificador nuevo.
   *
   * @return un {@link DonacionId} con un UUID aleatorio
   */
  public static DonacionId generar() {
    return new DonacionId(UUID.randomUUID());
  }
}
