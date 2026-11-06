package ar.edu.utn.donatrack.seguimiento;

/** Logística no respondió o respondió con error al consultar la posición de un camión. */
public class LogisticaNoDisponibleException extends RuntimeException {

  public LogisticaNoDisponibleException(String message) {
    super(message);
  }

  public LogisticaNoDisponibleException(String message, Throwable cause) {
    super(message, cause);
  }
}
