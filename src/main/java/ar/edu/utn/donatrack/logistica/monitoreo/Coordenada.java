package ar.edu.utn.donatrack.logistica.monitoreo;

/**
 * Representa una coordenada geográfica con latitud y longitud.
 *
 * <p>Value object inmutable con validación de rango y NaN en el compact constructor del record.
 * Latitud en [-90, 90], longitud en [-180, 180].
 *
 * @param latitud latitud en grados decimales
 * @param longitud longitud en grados decimales
 */
public record Coordenada(double latitud, double longitud) {

  public Coordenada {
    if (Double.isNaN(latitud) || Double.isNaN(longitud)) {
      throw new IllegalArgumentException(
          "coordenada no debe ser NaN: lat=" + latitud + ", lng=" + longitud);
    }
    if (latitud < -90 || latitud > 90) {
      throw new IllegalArgumentException("latitud fuera de rango [-90, 90]: " + latitud);
    }
    if (longitud < -180 || longitud > 180) {
      throw new IllegalArgumentException("longitud fuera de rango [-180, 180]: " + longitud);
    }
  }
}
