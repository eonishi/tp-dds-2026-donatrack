package ar.edu.utn.donatrack.security;

import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

public final class SecurityRules {
  private static final String API_KEY =
      System.getenv()
          .getOrDefault(
              "DONATRACK_API_KEY", System.getenv().getOrDefault("DONACIONES_API_KEY", ""));

  private SecurityRules() {}

  public static void enforceApi(Context ctx, AuthService authService) {
    if (isServiceRequest(ctx) && ctx.path().startsWith("/api/v1/donaciones")) {
      return;
    }
    Role[] required = requiredRolesFor(ctx.path(), ctx.method().toString());
    if (required == null) {
      return;
    }
    Optional<Usuario> current = authService.currentUser(ctx);
    if (current.isEmpty()) {
      throw new UnauthorizedResponse("Debe iniciar sesión para acceder a esta operación.");
    }
    Usuario usuario = current.get();
    if (usuario.hasRole(Role.ADMIN)) {
      return;
    }
    if (!usuario.hasAnyRole(required)) {
      throw new ForbiddenResponse("No tiene permisos para esta operación.");
    }
  }

  public static boolean isServiceRequest(Context ctx) {
    return isValidServiceKey(ctx.header("Authorization"), API_KEY);
  }

  static boolean isValidServiceKey(String headerValue, String configuredKey) {
    if (configuredKey == null
        || configuredKey.isBlank()
        || headerValue == null
        || headerValue.isBlank()) {
      return false;
    }
    return MessageDigest.isEqual(
        configuredKey.getBytes(StandardCharsets.UTF_8),
        headerValue.getBytes(StandardCharsets.UTF_8));
  }

  public static void enforceProtectedWeb(Context ctx, AuthService authService) {
    if (authService.currentUser(ctx).isEmpty()) {
      ctx.redirect("/login");
      return;
    }
  }

  public static Role[] requiredRolesFor(String path, String method) {
    if (path.startsWith("/api/v1/donantes")) {
      if ("GET".equals(method) && "/api/v1/donantes".equals(path)) {
        return new Role[] {Role.ADMIN};
      }
      if ("POST".equals(method) && "/api/v1/donantes".equals(path)) {
        return new Role[] {Role.ADMIN, Role.DONANTE};
      }
      return new Role[] {Role.ADMIN, Role.DONANTE};
    }
    if (path.startsWith("/api/v1/beneficiarias")) {
      if ("GET".equals(method) && "/api/v1/beneficiarias".equals(path)) {
        return new Role[] {Role.ADMIN, Role.BENEFICIARIO};
      }
      if ("POST".equals(method) && "/api/v1/beneficiarias".equals(path)) {
        return new Role[] {Role.ADMIN, Role.BENEFICIARIO};
      }
      return new Role[] {Role.ADMIN, Role.BENEFICIARIO};
    }
    if (path.startsWith("/api/v1/donaciones")) {
      if (path.endsWith("/estado")) {
        if ("PATCH".equals(method)) {
          return new Role[] {Role.ADMIN, Role.REPARTIDOR, Role.DONANTE};
        }
      }
      return new Role[] {Role.ADMIN, Role.DONANTE, Role.BENEFICIARIO, Role.REPARTIDOR};
    }
    if (path.startsWith("/api/v1/entidades/") && path.contains("/necesidades")) {
      return new Role[] {Role.ADMIN, Role.BENEFICIARIO};
    }
    if (path.startsWith("/api/v1/propuestas-asignacion")
        || path.contains("/propuesta-asignacion")) {
      return new Role[] {Role.ADMIN, Role.BENEFICIARIO};
    }
    if (path.startsWith("/api/v1/subcategorias")) {
      if ("GET".equals(method)) {
        return new Role[] {Role.ADMIN, Role.DONANTE, Role.BENEFICIARIO};
      }
      return new Role[] {Role.ADMIN};
    }
    return null;
  }
}
