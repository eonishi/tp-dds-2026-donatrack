package ar.edu.utn.donatrack.web.controller;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.DonantesRepository;
import ar.edu.utn.donatrack.security.AuthService;
import ar.edu.utn.donatrack.security.Usuario;
import io.javalin.http.Context;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Activación de las cuentas que crea la administración (alta en el depósito o importación CSV): la
 * persona prueba quién es con su correo y su número de documento, y elige su contraseña.
 */
public class ActivacionController {
  private static final int LARGO_MINIMO = 8;

  private final AuthService authService;
  private final DonantesRepository donantes;

  public ActivacionController(AuthService authService, DonantesRepository donantes) {
    this.authService = authService;
    this.donantes = donantes;
  }

  public void routes() {
    get(
        "/activar",
        ctx ->
            ctx.render(
                "activar",
                modelo(Optional.ofNullable(ctx.queryParam("correo")).orElse("").trim(), null)));
    post("/activar", this::activar);
  }

  private void activar(Context ctx) {
    String correo = Optional.ofNullable(ctx.formParam("email")).orElse("").trim();
    String documento = Optional.ofNullable(ctx.formParam("documento")).orElse("");
    String contrasena = Optional.ofNullable(ctx.formParam("password")).orElse("");
    if (contrasena.length() < LARGO_MINIMO) {
      ctx.status(400)
          .render(
              "activar", modelo(correo, "La contraseña tiene que tener al menos 8 caracteres."));
      return;
    }
    if (!contrasena.equals(ctx.formParam("confirmacion"))) {
      ctx.status(400).render("activar", modelo(correo, "Las contraseñas no coinciden."));
      return;
    }
    Optional<Usuario> activado =
        authService.activar(
            correo,
            documento,
            contrasena,
            id -> donantes.findById(id).map(Donante::getNumDocumento));
    if (activado.isEmpty()) {
      // Mismo mensaje para "no existe" y "no coincide": no revela qué correos tienen cuenta.
      ctx.status(400)
          .render(
              "activar",
              modelo(
                  correo,
                  "No encontramos una cuenta para activar con ese correo y documento. Si ya la"
                      + " activaste, ingresá con tu contraseña."));
      return;
    }
    authService.abrirSesion(ctx, activado.get());
    ctx.redirect("/dashboard");
  }

  private static Map<String, Object> modelo(String correo, String error) {
    Map<String, Object> modelo = new HashMap<>();
    modelo.put("email", correo);
    if (error != null) {
      modelo.put("error", error);
    }
    return modelo;
  }
}
