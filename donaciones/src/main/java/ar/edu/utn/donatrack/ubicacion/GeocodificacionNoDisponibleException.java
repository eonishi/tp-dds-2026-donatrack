package ar.edu.utn.donatrack.ubicacion;

/** El servicio de geocodificación no respondió: la dirección se vuelve a intentar más tarde. */
public class GeocodificacionNoDisponibleException extends RuntimeException {
  public GeocodificacionNoDisponibleException(String mensaje, Throwable causa) {
    super(mensaje, causa);
  }
}
