package ar.edu.utn.donatrack.notificaciones;

import java.util.Objects;

/**
 * Registro de un envío de una notificación: por qué canal salió y qué respondió el proveedor. El
 * destinatario es el de la {@link Notificacion}; el medio de contacto no se guarda.
 *
 * @param canal canal usado
 * @param resultado resultado informado por el proveedor
 */
public record Envio(Canal canal, ResultadoEnvio resultado) {
  public Envio {
    Objects.requireNonNull(canal);
    Objects.requireNonNull(resultado);
  }
}
