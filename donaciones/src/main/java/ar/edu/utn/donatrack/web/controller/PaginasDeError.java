package ar.edu.utn.donatrack.web.controller;

import ar.edu.utn.donatrack.security.AuthService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.HashMap;
import java.util.Map;

/**
 * Páginas de error propias de la web (400, 401, 403, 404, 405, 409 y 500), renderizadas dentro del
 * layout para que la persona pueda volver al inicio o a su panel.
 *
 * <p>Solo aplican fuera de {@code /api}: la API sigue respondiendo sus errores en JSON (ver {@code
 * ApiExceptionHandler}).
 */
public final class PaginasDeError {
  private final AuthService authService;

  private PaginasDeError(AuthService authService) {
    this.authService = authService;
  }

  public static void register(Javalin app, AuthService authService) {
    PaginasDeError paginas = new PaginasDeError(authService);
    paginas.registrar(
        app,
        400,
        "No pudimos procesar el pedido",
        "Algunos datos no son válidos. Volvé atrás, revisalos y probá de nuevo.");
    paginas.registrar(
        app,
        401,
        "Tenés que iniciar sesión",
        "Tu sesión no está iniciada o venció. Ingresá de nuevo para continuar.");
    paginas.registrar(
        app,
        403,
        "No tenés acceso a esta sección",
        "Tu cuenta no tiene permisos para ver esta página.");
    paginas.registrar(
        app,
        404,
        "No encontramos esta página",
        "La dirección no existe o cambió. Revisá el enlace o volvé al inicio.");
    paginas.registrar(
        app,
        405,
        "Esta acción no está disponible",
        "La página no admite este tipo de pedido. Volvé atrás y usá los botones del sitio.");
    paginas.registrar(
        app,
        409,
        "No se pudo completar la acción",
        "El estado cambió mientras operabas. Actualizá la página y probá de nuevo.");
    paginas.registrar(
        app,
        500,
        "Algo salió mal",
        "Tuvimos un problema al cargar la página. Probá de nuevo en unos minutos.");
  }

  private void registrar(Javalin app, int codigo, String titulo, String mensaje) {
    app.error(codigo, ctx -> mostrar(ctx, codigo, titulo, mensaje));
  }

  /** Las rutas de la API responden en JSON; todo lo demás es web. */
  public static boolean esApi(Context ctx) {
    return ctx.path().startsWith("/api/");
  }

  /**
   * Un controlador que ya renderizó su vista (por ejemplo, el registro con un error) la conserva.
   */
  private static boolean yaTieneVista(Context ctx) {
    String tipo = ctx.res().getContentType();
    return tipo != null && tipo.startsWith("text/html");
  }

  private void mostrar(Context ctx, int codigo, String titulo, String mensaje) {
    if (esApi(ctx) || yaTieneVista(ctx)) {
      return;
    }
    Map<String, Object> modelo = new HashMap<>();
    modelo.put("codigo", codigo);
    modelo.put("titulo", titulo);
    modelo.put("mensaje", mensaje);
    try {
      authService.currentUser(ctx).ifPresent(usuario -> modelo.put("usuario", usuario));
      ctx.status(codigo).render("error", modelo);
    } catch (RuntimeException e) {
      // Si la página de error tampoco se puede armar, queda un texto simple: nunca un stacktrace.
      ctx.status(codigo).contentType("text/plain; charset=utf-8").result(titulo + ". " + mensaje);
    }
  }
}
