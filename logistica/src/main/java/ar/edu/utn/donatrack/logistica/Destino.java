package ar.edu.utn.donatrack.logistica;

import java.util.List;
import java.util.Objects;

/**
 * Parada de una {@link Ruta}: agrupa las donaciones que se entregan en una misma entidad
 * beneficiaria.
 *
 * <p>Value object inmutable. {@code entidadId} identifica a la parada dentro de la ruta — una ruta
 * no puede repetir entidad beneficiaria (ver {@link Ruta}); {@code direccion} es una foto de la
 * dirección tomada al planificar y {@code donaciones} son las donaciones a entregar en ese orden.
 *
 * <p>Como value object, dos destinos son iguales cuando coinciden todos sus componentes: la
 * dirección y las donaciones son parte del valor, no de la identidad.
 *
 * <p>La entidad y la dirección viven en el microservicio de donaciones: acá llegan como valores del
 * contrato de integración, no se revalidan.
 *
 * @param entidadId entidad beneficiaria que recibe la entrega (no nula)
 * @param direccion dirección de la entrega (no nula)
 * @param donaciones donaciones a entregar en esta parada (no nula)
 */
public record Destino(
    EntidadBeneficiariaId entidadId, String direccion, List<DonacionId> donaciones) {
  public Destino {
    Objects.requireNonNull(entidadId, "entidadId no debe ser null");
    Objects.requireNonNull(direccion, "direccion no debe ser null");
    Objects.requireNonNull(donaciones, "donaciones no debe ser null");
    donaciones = List.copyOf(donaciones);
  }
}
