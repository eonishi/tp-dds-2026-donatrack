package ar.edu.utn.donatrack.api.controller;

import ar.edu.utn.donatrack.api.error.ApiException;
import io.javalin.http.Context;
import java.util.UUID;

final class HttpSupport {
  private HttpSupport() {}

  static UUID id(Context ctx, String parameter) {
    try {
      return UUID.fromString(ctx.pathParam(parameter));
    } catch (IllegalArgumentException exception) {
      throw ApiException.badRequest(parameter + " debe ser un UUID válido.");
    }
  }

  static long longId(Context ctx, String parameter) {
    try {
      return Long.parseLong(ctx.pathParam(parameter));
    } catch (NumberFormatException exception) {
      throw ApiException.badRequest(parameter + " debe ser un número entero válido.");
    }
  }
}
