package ar.edu.utn.donatrack.security;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import java.util.HashMap;
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
    get("/catalogo", ctx -> panel(ctx, "catalogo", "Catálogo"));
    get("/matchmaking", ctx -> panel(ctx, "matchmaking", "Asignación de donaciones"));
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
      // Se conserva el correo para no tener que reescribirlo; la contraseña nunca vuelve.
      ctx.render(
          "login",
          Map.of("error", "Correo o contraseña incorrectos.", "email", email == null ? "" : email));
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
    Map<String, Object> modelo = new HashMap<>();
    modelo.put("usuario", usuario);
    modelo.put("seccion", seccion);
    modelo.put("titulo", titulo);
    // Una entidad opera sobre sus propias necesidades: el formulario no le pide elegir entidad.
    if (!usuario.hasRole(Role.ADMIN)) {
      usuario.getEntidadId().ifPresent(id -> modelo.put("entidadId", id));
    }
    ctx.render("panel", modelo);
  }
}
