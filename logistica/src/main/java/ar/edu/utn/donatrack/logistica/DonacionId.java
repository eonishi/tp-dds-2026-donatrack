package ar.edu.utn.donatrack.logistica;

import java.util.Objects;
import java.util.UUID;
import javax.persistence.Embeddable;

/**
 * Identifica una donación, tal como la nombra el contrato de integración ({@code idDonacion}, un
 * UUID).
 *
 * <p>Es solo una referencia: la donación y su ciclo de vida viven en el microservicio de
 * donaciones.
 *
 * @param value valor del identificador (no nulo)
 */
@Embeddable
public record DonacionId(UUID value) {
  public DonacionId {
    Objects.requireNonNull(value, "value no debe ser null");
  }
}
