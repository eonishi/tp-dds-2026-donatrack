package ar.edu.utn.donatrack.security;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

import io.javalin.http.Context;
import io.javalin.security.RouteRole;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class AuthController {
  private final AuthService authService;
  private final boolean demoDataEnabled;

  public AuthController(AuthService authService) {
    this(authService, "true".equalsIgnoreCase(System.getenv("DONATRACK_DEMO_DATA")));
  }

  public AuthController(AuthService authService, boolean demoDataEnabled) {
    this.authService = authService;
    this.demoDataEnabled = demoDataEnabled;
  }

  // Cualquier persona con sesión, sea cual sea su rol.
  private static final RouteRole[] CON_SESION = {Role.ADMIN, Role.DONANTE, Role.BENEFICIARIO};

  public void routes() {
    get("/login", this::loginPage);
    post("/login", this::login);
    post("/logout", this::logout);
    get("/dashboard", this::dashboard, CON_SESION);
    get("/perfil", this::dashboard, CON_SESION);
    get("/donaciones", ctx -> panel(ctx, "donaciones", "Donaciones"), CON_SESION);
    get("/necesidades", ctx -> panel(ctx, "necesidades", "Necesidades"), CON_SESION);
    // Los repartidores no usan esta web: operan desde su aplicación contra logística.
    get("/logistica", ctx -> panel(ctx, "logistica", "Logística"), Role.ADMIN);
    get("/catalogo", ctx -> panel(ctx, "catalogo", "Catálogo"), Role.ADMIN);
    get("/matchmaking", ctx -> panel(ctx, "matchmaking", "Asignación de donaciones"), Role.ADMIN);
  }

  private void loginPage(Context ctx) {
    if (authService.currentUser(ctx).isPresent()) {
      ctx.redirect("/dashboard");
      return;
    }
    ctx.render("login", Map.of("error", "", "demoDataEnabled", demoDataEnabled));
  }

  private void login(Context ctx) {
    String email = ctx.formParam("email");
    String password = ctx.formParam("password");
    Optional<Usuario> usuario = authService.login(ctx, email, password);
    if (usuario.isEmpty()) {
      // Se conserva el correo para no tener que reescribirlo; la contraseña nunca vuelve.
      ctx.render(
          "login",
          Map.of(
              "error",
              "Correo o contraseña incorrectos.",
              "email",
              email == null ? "" : email,
              "demoDataEnabled",
              demoDataEnabled));
      return;
    }
    ctx.redirect("/dashboard");
  }

  private void logout(Context ctx) {
    authService.logout(ctx);
    ctx.redirect("/");
  }

  private void dashboard(Context ctx) {
    ctx.render("dashboard", Map.of("usuario", ControlDeAcceso.usuarioObligatorio(ctx)));
  }

  private void panel(Context ctx, String seccion, String titulo) {
    Usuario usuario = ControlDeAcceso.usuarioObligatorio(ctx);
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
