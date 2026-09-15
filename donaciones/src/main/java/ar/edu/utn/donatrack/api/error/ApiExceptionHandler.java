package ar.edu.utn.donatrack.api.error;

import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

public final class ApiExceptionHandler {
  private ApiExceptionHandler() {}

  public static void register(Javalin app, Clock clock) {
    app.exception(ApiException.class, (exception, ctx) -> write(ctx, clock, exception));
    app.exception(
        BadRequestResponse.class,
        (exception, ctx) ->
            write(
                ctx,
                clock,
                new ApiException(
                    400, "MALFORMED_REQUEST", "El cuerpo o los parámetros son inválidos.")));
    app.exception(
        IllegalArgumentException.class,
        (exception, ctx) ->
            write(ctx, clock, new ApiException(400, "INVALID_ARGUMENT", exception.getMessage())));
    app.exception(
        IllegalStateException.class,
        (exception, ctx) ->
            write(ctx, clock, new ApiException(409, "INVALID_STATE", exception.getMessage())));
    app.exception(
        Exception.class,
        (exception, ctx) ->
            write(
                ctx,
                clock,
                new ApiException(
                    500, "INTERNAL_ERROR", "Ocurrió un error interno al procesar la solicitud.")));
    app.error(
        404,
        ctx -> {
          if (ctx.contentType() == null) {
            write(
                ctx,
                clock,
                new ApiException(404, "ROUTE_NOT_FOUND", "La ruta solicitada no existe."));
          }
        });
    app.error(
        405,
        ctx -> {
          if (ctx.contentType() == null) {
            write(
                ctx,
                clock,
                new ApiException(405, "METHOD_NOT_ALLOWED", "El método HTTP no está permitido."));
          }
        });
  }

  private static void write(Context ctx, Clock clock, ApiException exception) {
    ctx.status(exception.getStatus())
        .json(
            new ApiError(
                Instant.now(clock),
                exception.getStatus(),
                reason(exception.getStatus()),
                exception.getCode(),
                exception.getMessage(),
                ctx.path(),
                exception.getDetails() == null ? List.of() : exception.getDetails()));
  }

  private static String reason(int status) {
    return switch (status) {
      case 400 -> "Bad Request";
      case 404 -> "Not Found";
      case 405 -> "Method Not Allowed";
      case 409 -> "Conflict";
      default -> "Internal Server Error";
    };
  }
}
