package ar.edu.utn.donatrack.logistica.planificacion;

import ar.edu.utn.donatrack.logistica.Camion;
import java.util.List;
import java.util.Objects;

/**
 * Lote de donaciones a planificar junto con los camiones disponibles.
 *
 * @param donaciones donaciones a planificar (no nulas, máximo {@value #MAXIMO_DONACIONES})
 * @param camiones camiones disponibles para el lote (no nulos, al menos uno)
 */
public record LoteDonaciones(List<DonacionPlanificable> donaciones, List<Camion> camiones) {

  /** Tope de donaciones por envío al planificador externo. */
  public static final int MAXIMO_DONACIONES = 100;

  public LoteDonaciones {
    Objects.requireNonNull(donaciones, "donaciones no debe ser null");
    Objects.requireNonNull(camiones, "camiones no debe ser null");
    if (camiones.isEmpty()) {
      throw new IllegalArgumentException("lote debe tener al menos un camión");
    }
    if (donaciones.size() > MAXIMO_DONACIONES) {
      throw new IllegalArgumentException(
          "lote no puede exceder " + MAXIMO_DONACIONES + " donaciones");
    }
  }
}
