package ar.edu.utn.donatrack.ubicacion;

import java.util.Optional;

/** Convierte una dirección escrita en coordenadas para los mapas. */
public interface Geocodificador {
  /**
   * @return las coordenadas, o vacío si la dirección no se encontró
   * @throws GeocodificacionNoDisponibleException si el servicio no respondió (se reintenta más
   *     tarde)
   */
  Optional<Coordenadas> ubicar(String direccion);
}
