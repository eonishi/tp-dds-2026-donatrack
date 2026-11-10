package ar.edu.utn.donatrack.web.controller;

import io.javalin.http.Context;
import java.util.regex.Pattern;

/** Lectura y validación de campos de formularios HTML, comunes a las vistas. */
final class Formularios {
  private static final Pattern CORREO = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");

  private Formularios() {}

  static String obligatorio(Context ctx, String campo, String nombre) {
    String valor = ctx.formParam(campo);
    if (valor == null || valor.isBlank()) {
      throw new DatoInvalido("Falta completar " + nombre + ".");
    }
    return valor;
  }

  static <E extends Enum<E>> E enumerado(Class<E> tipo, String valor, String nombre) {
    try {
      return Enum.valueOf(tipo, valor == null ? "" : valor.trim());
    } catch (IllegalArgumentException e) {
      throw new DatoInvalido("Elegí " + nombre + ".");
    }
  }

  /** El correo del campo {@code campo}, en minúsculas y con formato válido. */
  static String correo(Context ctx, String campo) {
    String correo = obligatorio(ctx, campo, "el correo electrónico").trim().toLowerCase();
    if (!CORREO.matcher(correo).matches()) {
      throw new DatoInvalido("El correo electrónico no es válido.");
    }
    return correo;
  }

  /** Un dato del formulario que no se puede aceptar; se muestra en la misma página. */
  static final class DatoInvalido extends RuntimeException {
    DatoInvalido(String mensaje) {
      super(mensaje);
    }
  }
}
