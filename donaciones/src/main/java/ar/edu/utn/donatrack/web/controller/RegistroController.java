package ar.edu.utn.donatrack.web.controller;

import static ar.edu.utn.donatrack.web.controller.Formularios.obligatorio;
import static io.javalin.apibuilder.ApiBuilder.post;

import ar.edu.utn.donatrack.beneficiarios.BeneficiariosRepository;
import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.DonantesRepository;
import ar.edu.utn.donatrack.notificaciones.ServicioNotificaciones;
import ar.edu.utn.donatrack.notificaciones.eventos.EventoBienvenida;
import ar.edu.utn.donatrack.security.AuthService;
import ar.edu.utn.donatrack.security.Role;
import ar.edu.utn.donatrack.security.Usuario;
import ar.edu.utn.donatrack.web.controller.Formularios.DatoInvalido;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.http.Context;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Alta de cuentas desde la página de registro (entrega 4, punto 3).
 *
 * <p>Crea la persona donante o la entidad beneficiaria y su usuario ya vinculado en una sola
 * transacción: si algo falla no queda ninguno de los dos. Los ids los asigna el servicio, nunca los
 * manda el formulario. Al terminar deja la sesión iniciada.
 */
public final class RegistroController implements WithSimplePersistenceUnit {
  private static final int LARGO_MINIMO_CONTRASENA = 8;

  private final AuthService authService;
  private final DonantesRepository donantes;
  private final BeneficiariosRepository entidades;
  private final ServicioNotificaciones notificaciones;

  public RegistroController(
      AuthService authService,
      DonantesRepository donantes,
      BeneficiariosRepository entidades,
      ServicioNotificaciones notificaciones) {
    this.authService = authService;
    this.donantes = donantes;
    this.entidades = entidades;
    this.notificaciones = notificaciones;
  }

  public void routes() {
    post("/registro", this::registrar);
  }

  private void registrar(Context ctx) {
    try {
      String correo = Formularios.correo(ctx, "correo");
      String contrasena = validarContrasena(ctx);
      if (ctx.formParam("terminos") == null) {
        throw new DatoInvalido("Tenés que aceptar los términos y la política de privacidad.");
      }
      if (authService.existeUsuario(correo)) {
        throw new DatoInvalido("Ya hay una cuenta con ese correo electrónico.");
      }
      Usuario usuario =
          "entidad".equals(ctx.formParam("rol"))
              ? registrarEntidad(ctx, correo, contrasena)
              : registrarDonante(ctx, correo, contrasena);
      usuario
          .getDonanteId()
          .flatMap(donantes::findById)
          .ifPresent(
              donante ->
                  notificaciones.enviar(
                      new EventoBienvenida(donante, enlaceIngreso(ctx)).generarNotificaciones()));
      usuario
          .getEntidadId()
          .flatMap(entidades::findById)
          .ifPresent(
              entidad ->
                  notificaciones.enviar(
                      new EventoBienvenida(entidad, enlaceIngreso(ctx)).generarNotificaciones()));
      authService.abrirSesion(ctx, usuario);
      ctx.redirect("/dashboard");
    } catch (DatoInvalido e) {
      ctx.status(400);
      ctx.render("registro", modeloConError(ctx, e.getMessage()));
    }
  }

  private Usuario registrarDonante(Context ctx, String correo, String contrasena) {
    Donante persona = FormularioDonante.armar(ctx, correo, donantes);
    return withTransaction(
        () -> {
          var guardada = donantes.save(persona);
          return authService.agregarUsuario(
              correo, contrasena, Set.of(Role.DONANTE), guardada.getId(), null);
        });
  }

  private Usuario registrarEntidad(Context ctx, String correo, String contrasena) {
    String razonSocial = obligatorio(ctx, "entidadRazonSocial", "la razón social").trim();
    String direccion = obligatorio(ctx, "entidadDireccion", "la dirección");
    String telefono = obligatorio(ctx, "entidadTelefono", "el teléfono");
    boolean razonUsada =
        entidades.getBeneficiarios().stream()
            .anyMatch(e -> e.getRazonSocial().equalsIgnoreCase(razonSocial));
    if (razonUsada) {
      throw new DatoInvalido("Ya hay una entidad registrada con esa razón social.");
    }

    EntidadBeneficiaria entidad = new EntidadBeneficiaria(razonSocial, direccion, telefono);
    entidad.actualizarDatos(
        razonSocial, direccion, telefono, List.of(new CorreoElectronico(correo)));

    return withTransaction(
        () -> {
          EntidadBeneficiaria guardada = entidades.save(entidad);
          return authService.agregarUsuario(
              correo, contrasena, Set.of(Role.BENEFICIARIO), null, guardada.getId());
        });
  }

  private static String enlaceIngreso(Context ctx) {
    return Vistas.urlPublica(ctx, "/login");
  }

  private static String validarContrasena(Context ctx) {
    String contrasena = obligatorio(ctx, "contrasena", "la contraseña");
    if (contrasena.length() < LARGO_MINIMO_CONTRASENA) {
      throw new DatoInvalido(
          "La contraseña tiene que tener al menos " + LARGO_MINIMO_CONTRASENA + " caracteres.");
    }
    if (!contrasena.equals(ctx.formParam("contrasenaRepetida"))) {
      throw new DatoInvalido("Las contraseñas no coinciden.");
    }
    return contrasena;
  }

  private Map<String, Object> modeloConError(Context ctx, String error) {
    Map<String, Object> modelo = new HashMap<>();
    authService.currentUser(ctx).ifPresent(usuario -> modelo.put("usuario", usuario));
    modelo.put("error", error);
    return modelo;
  }
}
