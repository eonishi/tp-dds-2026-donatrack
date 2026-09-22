package ar.edu.utn.donatrack.beneficiarios;

import java.util.Objects;

/**
 * Identifica una entidad beneficiaria de forma única, también al cruzar al microservicio de
 * logística.
 *
 * <p>El contrato de integración fija {@code idEntidadBeneficiaria} como {@link Long}: son pocas y
 * centralizadas, así que la asignación queda en el borde del servicio (persistencia o alta
 * administrativa) y no en una secuencia global en memoria.
 *
 * @param value valor del identificador (no nulo)
 */
public record EntidadBeneficiariaId(Long value) {
  public EntidadBeneficiariaId {
    Objects.requireNonNull(value, "value no debe ser null");
  }
}
