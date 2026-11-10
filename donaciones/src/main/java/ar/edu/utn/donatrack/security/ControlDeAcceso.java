package ar.edu.utn.donatrack.security;

import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.security.RouteRole;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.Set;

/**
 * El único control de acceso de donaciones. Cada ruta declara quién puede usarla al registrarse,
 * por ejemplo {@code get("/admin/donantes", this::listar, Role.ADMIN)}, y este filtro lo hace
 * cumplir antes de que corra el handler:
 *
 * <ul>
 *   <li>Sin roles declarados, la ruta es pública.
 *   <li>Con {@link ServicioInterno#LOGISTICA}, también entra logística con la clave interna.
 *   <li>Sin sesión: la web redirige al login y la API responde 401.
 *   <li>Con sesión pero sin ninguno de los roles: 403. La administración no pasa a todos lados:
 *       entra solo donde la ruta la nombra.
 * </ul>
 *
 * <p>Quien pasa queda en el request ({@link #usuario}): los handlers no vuelven a buscarlo.
 */
public final class ControlDeAcceso {
  private static final String ATRIBUTO_USUARIO = "usuario";

  // Clave interna con la que logística consulta las donaciones a planificar y les cambia el
  // estado al iniciar una ruta. Sin configurar queda desactivada.
  private static final String CLAVE_INTERNA =
      System.getenv()
          .getOrDefault(
              "DONATRACK_API_KEY", System.getenv().getOrDefault("DONACIONES_API_KEY", ""));

  private ControlDeAcceso() {}

  /** Filtro {@code beforeMatched}: corre una vez por request, con los roles de la ruta. */
  public static void verificar(Context ctx, AuthService authService) {
    Set<RouteRole> permitidos = ctx.routeRoles();
    if (permitidos.isEmpty()) {
      return;
    }
    if (permitidos.contains(ServicioInterno.LOGISTICA) && esLogistica(ctx)) {
      return;
    }
    Optional<Usuario> conSesion = authService.currentUser(ctx);
    if (conSesion.isEmpty()) {
      if (ctx.path().startsWith("/api/")) {
        throw new UnauthorizedResponse("Debe iniciar sesión para acceder a esta operación.");
      }
      ctx.redirect("/login");
      ctx.skipRemainingHandlers();
      return;
    }
    Usuario usuario = conSesion.get();
    if (usuario.getRoles().stream().noneMatch(permitidos::contains)) {
      throw new ForbiddenResponse("No tiene permisos para esta operación.");
    }
    ctx.attribute(ATRIBUTO_USUARIO, usuario);
  }

  /**
   * Quien hace el request, ya verificado por el filtro. Vacío solo en rutas públicas o si es
   * logística con la clave interna.
   */
  public static Optional<Usuario> usuario(Context ctx) {
    return Optional.ofNullable(ctx.attribute(ATRIBUTO_USUARIO));
  }

  /** Como {@link #usuario}, para las rutas que exigen sesión: ahí siempre hay alguien. */
  public static Usuario usuarioObligatorio(Context ctx) {
    return usuario(ctx).orElseThrow(() -> new UnauthorizedResponse("Sin sesión."));
  }

  private static boolean esLogistica(Context ctx) {
    return claveValida(ctx.header("Authorization"), CLAVE_INTERNA);
  }

  /** Comparación en tiempo constante, para no filtrar la clave por cuánto tarda en fallar. */
  static boolean claveValida(String recibida, String configurada) {
    if (configurada == null || configurada.isBlank() || recibida == null || recibida.isBlank()) {
      return false;
    }
    return MessageDigest.isEqual(
        configurada.getBytes(StandardCharsets.UTF_8), recibida.getBytes(StandardCharsets.UTF_8));
  }
}
