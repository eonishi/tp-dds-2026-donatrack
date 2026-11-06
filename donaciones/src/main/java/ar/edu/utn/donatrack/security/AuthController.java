package ar.edu.utn.donatrack.security;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import java.util.Map;
import java.util.Optional;

public final class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  public void routes() {
    get("/login", this::loginPage);
    post("/login", this::login);
    post("/logout", this::logout);
    get("/dashboard", this::dashboard);
    get("/perfil", this::dashboard);
    get("/donaciones", ctx -> panel(ctx, "donaciones", "Donaciones"));
    get("/necesidades", ctx -> panel(ctx, "necesidades", "Necesidades"));
    get("/logistica", ctx -> panel(ctx, "logistica", "Logística"));
    get("/matchmaking", ctx -> panel(ctx, "matchmaking", "Asignación de donaciones"));
    get("/catalogo", ctx -> panel(ctx, "catalogo", "Catálogo de subcategorías"));
  }

  private void loginPage(Context ctx) {
    if (authService.currentUser(ctx).isPresent()) {
      ctx.redirect("/dashboard");
      return;
    }
    ctx.render("login", Map.of("error", ""));
  }

  private void login(Context ctx) {
    String email = ctx.formParam("email");
    String password = ctx.formParam("password");
    Optional<Usuario> usuario = authService.login(ctx, email, password);
    if (usuario.isEmpty()) {
      ctx.render("login", Map.of("error", "Credenciales inválidas."));
      return;
    }
    ctx.redirect("/dashboard");
  }

  private void logout(Context ctx) {
    authService.logout(ctx);
    ctx.redirect("/");
  }

  private void dashboard(Context ctx) {
    Usuario usuario = authService.currentUser(ctx).orElse(null);
    if (usuario == null) {
      ctx.redirect("/login");
      return;
    }
    ctx.render("dashboard", Map.of("usuario", usuario));
  }

  private void panel(Context ctx, String seccion, String titulo) {
    Usuario usuario = authService.currentUser(ctx).orElse(null);
    if (usuario == null) {
      ctx.redirect("/login");
      return;
    }
    // Los repartidores no usan esta web: operan desde su aplicación contra logística.
    if (("logistica".equals(seccion) || "matchmaking".equals(seccion) || "catalogo".equals(seccion))
        && !usuario.hasRole(Role.ADMIN)) {
      throw new ForbiddenResponse("Sólo administradores pueden acceder a esta sección.");
    }
    ctx.render("panel", Map.of("usuario", usuario, "seccion", seccion, "titulo", titulo));
  }
}
