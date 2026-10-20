package ar.edu.utn.donatrack.web.controller;

import static io.javalin.apibuilder.ApiBuilder.get;

import io.javalin.http.Context;
import java.util.Map;

/**
 * Pantallas accesibles sin iniciar sesión: la landing y, por ahora, páginas "próximamente" para las
 * secciones que la landing enlaza y todavía no están implementadas.
 */
public final class PaginasPublicasController {

  public void routes() {
    get("/", this::landing);
    get("/legal", ctx -> proximamente(ctx, "Legal y privacidad"));
    get("/entregas", ctx -> proximamente(ctx, "Donaciones entregadas"));
    get("/login", ctx -> proximamente(ctx, "Iniciar sesión"));
    get("/registro", ctx -> proximamente(ctx, "Registro de personas donantes"));
    get("/registro-entidad", ctx -> proximamente(ctx, "Registro de entidades beneficiarias"));
  }

  private void landing(Context ctx) {
    ctx.render("index");
  }

  private void proximamente(Context ctx, String seccion) {
    ctx.render("proximamente", Map.of("seccion", seccion));
  }
}
