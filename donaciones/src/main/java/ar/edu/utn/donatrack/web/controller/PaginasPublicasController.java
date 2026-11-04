package ar.edu.utn.donatrack.web.controller;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

import io.javalin.http.Context;
import java.util.Map;

/**
 * Pantallas accesibles sin iniciar sesión: la landing y, por ahora, páginas "próximamente" para las
 * secciones que la landing enlaza y todavía no están implementadas.
 */
public final class PaginasPublicasController {

  public void routes() {
    get("/", this::landing);
    get("/legal", this::legal);
    get("/entregas", ctx -> proximamente(ctx, "Donaciones entregadas"));
    get("/login", ctx -> proximamente(ctx, "Iniciar sesión"));
    get("/registro", this::registro);
    post("/registro", ctx -> proximamente(ctx, "Registro"));
  }

  private void landing(Context ctx) {
    ctx.render("index");
  }

  private void legal(Context ctx) {
    ctx.render("legal");
  }

  private void registro(Context ctx) {
    ctx.render("registro");
  }

  private void proximamente(Context ctx, String seccion) {
    ctx.render("proximamente", Map.of("seccion", seccion));
  }
}
