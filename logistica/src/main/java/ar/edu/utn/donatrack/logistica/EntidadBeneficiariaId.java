package ar.edu.utn.donatrack.logistica;

import java.util.Objects;

/**
 * Identifica una entidad beneficiaria, tal como la nombra el contrato de integración ({@code
 * idEntidadBeneficiaria}, un {@link Long}).
 *
 * <p>Logística guarda el identificador y no la entidad: la entidad beneficiaria vive en el
 * microservicio de donaciones y acá solo llega como valor de la frontera.
 *
 * @param value valor del identificador (no nulo)
 */
public record EntidadBeneficiariaId(Long value) {
  public EntidadBeneficiariaId {
    Objects.requireNonNull(value, "value no debe ser null");
  }
}
