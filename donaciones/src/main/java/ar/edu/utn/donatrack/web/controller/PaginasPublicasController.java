package ar.edu.utn.donatrack.web.controller;

import static io.javalin.apibuilder.ApiBuilder.get;

import ar.edu.utn.donatrack.security.AuthService;
import io.javalin.http.Context;
import java.util.HashMap;
import java.util.Map;

/**
 * Pantallas accesibles sin iniciar sesión: la landing y las páginas públicas. El login y el panel
 * autenticado se manejan por AuthController con cookies JWT y sesión en memoria.
 */
public final class PaginasPublicasController {
  private final AuthService authService;

  public PaginasPublicasController(AuthService authService) {
    this.authService = authService;
  }

  public void routes() {
    get("/", this::landing);
    get("/legal", this::legal);
    get("/entregas", ctx -> proximamente(ctx, "Donaciones entregadas"));
    get("/registro", this::registro);
  }

  private void landing(Context ctx) {
    ctx.render("index", modeloPublico(ctx));
  }

  private void legal(Context ctx) {
    ctx.render("legal", modeloPublico(ctx));
  }

  private void registro(Context ctx) {
    ctx.render("registro", modeloPublico(ctx));
  }

  private void proximamente(Context ctx, String seccion) {
    Map<String, Object> modelo = modeloPublico(ctx);
    modelo.put("seccion", seccion);
    ctx.render("proximamente", modelo);
  }

  private Map<String, Object> modeloPublico(Context ctx) {
    Map<String, Object> modelo = new HashMap<>();
    authService.currentUser(ctx).ifPresent(usuario -> modelo.put("usuario", usuario));
    return modelo;
  }
}
