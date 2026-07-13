package ar.edu.utn.donatrack.monitoreo;

import ar.edu.utn.donatrack.logistica.Camion;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Representa una lectura de posición recibida de un camión.
 *
 * <p>Data carrier inmutable que transporta la identidad del camión, la coordenada geográfica, la
 * velocidad actual y un timestamp. El compact constructor rechaza argumentos {@code null}.
 *
 * @param camion el camión que reporta la posición (no nulo)
 * @param coordenada la coordenada geográfica (no nula)
 * @param velocidad la velocidad actual en km/h
 * @param timestamp la fecha y hora de la lectura (no nulo)
 */
public record LecturaPosicion(
    Camion camion, Coordenada coordenada, double velocidad, LocalDateTime timestamp) {

  public LecturaPosicion {
    Objects.requireNonNull(camion, "camion no debe ser nulo");
    Objects.requireNonNull(coordenada, "coordenada no debe ser nula");
    Objects.requireNonNull(timestamp, "timestamp no debe ser nulo");
  }
}
