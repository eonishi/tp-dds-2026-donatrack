package ar.edu.utn.donatrack.security;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

import io.javalin.http.Context;
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
}
