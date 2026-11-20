package ar.edu.utn.donatrack.donantes;

/**
 * El documento ya es de otra persona donante, según el chequeo previo ({@link
 * DonantesRepository#documentoEnUso}).
 *
 * <p>Es un {@link IllegalStateException} porque el dato es válido; lo que choca es el estado de lo
 * ya registrado.
 */
public class DocumentoEnUso extends IllegalStateException {
  public static final String MENSAJE = "Ya hay una persona donante registrada con ese documento.";

  public DocumentoEnUso() {
    super(MENSAJE);
  }
}
