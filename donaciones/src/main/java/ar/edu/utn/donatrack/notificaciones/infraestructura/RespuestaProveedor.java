package ar.edu.utn.donatrack.notificaciones.infraestructura;

final class RespuestaProveedor {
  private static final int MAX_BODY_LENGTH = 300;

  private RespuestaProveedor() {}

  static String error(String proveedor, int status, String body, String... secretos) {
    String sanitizado = body == null ? "" : body.replaceAll("[\\r\\n\\t]+", " ").trim();
    for (String secreto : secretos) {
      if (secreto != null && !secreto.isBlank()) {
        sanitizado = sanitizado.replace(secreto, "[REDACTADO]");
      }
    }
    if (sanitizado.length() > MAX_BODY_LENGTH) {
      sanitizado = sanitizado.substring(0, MAX_BODY_LENGTH) + "…";
    }
    return proveedor
        + " respondió HTTP "
        + status
        + (sanitizado.isEmpty() ? "." : ": " + sanitizado);
  }
}
