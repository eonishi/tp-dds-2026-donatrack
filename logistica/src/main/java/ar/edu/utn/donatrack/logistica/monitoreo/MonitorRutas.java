package ar.edu.utn.donatrack.logistica.monitoreo;

import ar.edu.utn.donatrack.logistica.Camion;
import ar.edu.utn.donatrack.logistica.RutaRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Servicio que procesa lecturas de posición de camiones en rutas activas.
 *
 * <p>Recibe {@link LecturaPosicion}, valida que el timestamp no sea futuro, correlaciona con la
 * ruta activa del camión mediante {@link RutaRepository#buscarPorCamion}, y mantiene la última
 * posición conocida por camión en un mapa interno.
 *
 * <p>Recibe un {@link Clock} para controlar la validación temporal (testeable mediante {@link
 * Clock#fixed}).
 */
public class MonitorRutas {

  private final Clock clock;
  private final RutaRepository rutaRepository;
  private final Map<Camion, LecturaPosicion> ultimasPosiciones;

  /**
   * Crea un monitor de rutas.
   *
   * @param clock reloj para validación de timestamp (no nulo)
   * @param rutaRepository repositorio de rutas para correlación (no nulo)
   */
  public MonitorRutas(Clock clock, RutaRepository rutaRepository) {
    this.clock = Objects.requireNonNull(clock, "clock no debe ser nulo");
    this.rutaRepository = Objects.requireNonNull(rutaRepository, "rutaRepository no debe ser nulo");
    this.ultimasPosiciones = new HashMap<>();
  }

  /**
   * Procesa una lectura de posición.
   *
   * <p>La lectura es aceptada si cumple todas las condiciones:
   *
   * <ol>
   *   <li>El timestamp no es posterior al instante actual del clock
   *   <li>El camión tiene una ruta activa (enCurso = true)
   * </ol>
   *
   * Si la lectura es aceptada, se actualiza la última posición del camión. En caso contrario, la
   * operación es no-op.
   *
   * @param lectura la lectura a procesar (no nula)
   */
  public void procesar(LecturaPosicion lectura) {
    Objects.requireNonNull(lectura, "lectura no debe ser nula");

    if (lectura.timestamp().isAfter(LocalDateTime.now(clock))) {
      return;
    }

    if (rutaRepository.buscarPorCamion(lectura.camion()).isEmpty()) {
      return;
    }

    ultimasPosiciones.put(lectura.camion(), lectura);
  }

  /**
   * Retorna la última posición conocida del camión, si existe.
   *
   * @param camion el camión a consultar (no nulo)
   * @return un {@link Optional} con la última {@link LecturaPosicion}, o {@link Optional#empty()}
   *     si el camión nunca reportó una lectura exitosa
   */
  public Optional<LecturaPosicion> ultimaPosicion(Camion camion) {
    Objects.requireNonNull(camion, "camion no debe ser nulo");
    return Optional.ofNullable(ultimasPosiciones.get(camion));
  }
}
