package ar.edu.utn.donatrack.logistica.planificacion;

import ar.edu.utn.donatrack.logistica.Camion;
import ar.edu.utn.donatrack.logistica.Destino;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Resultado que devuelve el planificador externo para un lote: por camión, los destinos a visitar.
 *
 * <p>El orden de la lista de destinos de cada camión es parte del contrato: el planificador decide
 * la secuencia de visitas y logística la respeta al materializar la {@link Ruta}.
 *
 * @param loteId lote al que corresponde el resultado (no nulo)
 * @param rutas destinos por camión (no nulo; puede estar vacío)
 * @param donacionesNoAsignadas identificadores de donaciones que el planificador no pudo ubicar en
 *     ningún camión
 */
public record ResultadoPlanificacion(
    UUID loteId, Map<Camion, List<Destino>> rutas, List<UUID> donacionesNoAsignadas) {
  public ResultadoPlanificacion {
    Objects.requireNonNull(loteId, "loteId no debe ser null");
    Objects.requireNonNull(rutas, "rutas no debe ser null");
    Objects.requireNonNull(donacionesNoAsignadas, "donacionesNoAsignadas no debe ser null");
  }
}
