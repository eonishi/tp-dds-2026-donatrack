package ar.edu.utn.donatrack.notificaciones;

import java.util.Objects;

public record ResultadoEnvio(boolean exitoso, String detalle) {
  public ResultadoEnvio {
    Objects.requireNonNull(detalle);
  }

  public static ResultadoEnvio aceptado() {
    return new ResultadoEnvio(true, "Aceptada por el proveedor.");
  }

  public static ResultadoEnvio fallido(String detalle) {
    return new ResultadoEnvio(false, detalle);
  }
}
