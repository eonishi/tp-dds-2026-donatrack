package ar.edu.utn.donatrack.logistica.graphql;

/**
 * Error de negocio de una operación GraphQL. Llega al cliente en {@code errors}, con el código en
 * {@code extensions.codigo}.
 */
public class ErrorLogistica extends RuntimeException {

  /** Qué salió mal, para que el cliente decida qué hacer sin leer el mensaje. */
  public enum Codigo {
    NO_ENCONTRADO,
    DATOS_INVALIDOS,
    PROHIBIDO
  }

  private final Codigo codigo;

  public ErrorLogistica(Codigo codigo, String mensaje) {
    super(mensaje);
    this.codigo = codigo;
  }

  public Codigo getCodigo() {
    return codigo;
  }
}
