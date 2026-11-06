package ar.edu.utn.donatrack.web.controller;

import static io.javalin.apibuilder.ApiBuilder.post;

import ar.edu.utn.donatrack.beneficiarios.BeneficiariosRepository;
import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.DonantesRepository;
import ar.edu.utn.donatrack.donantes.Genero;
import ar.edu.utn.donatrack.donantes.MedioDeContacto;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.Sms;
import ar.edu.utn.donatrack.donantes.TipoDocumento;
import ar.edu.utn.donatrack.donantes.Whatsapp;
import ar.edu.utn.donatrack.security.AuthService;
import ar.edu.utn.donatrack.security.Role;
import ar.edu.utn.donatrack.security.Usuario;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.http.Context;
import java.util.ArrayList;
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

  public RegistroController(
      AuthService authService, DonantesRepository donantes, BeneficiariosRepository entidades) {
    this.authService = authService;
    this.donantes = donantes;
    this.entidades = entidades;
  }

  public void routes() {
    post("/registro", this::registrar);
  }

  private void registrar(Context ctx) {
    try {
      String correo = obligatorio(ctx, "correo", "el correo electrónico").trim().toLowerCase();
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
      authService.abrirSesion(ctx, usuario);
      ctx.redirect("/dashboard");
    } catch (DatoInvalido e) {
      ctx.status(400);
      ctx.render("registro", modeloConError(ctx, e.getMessage()));
    }
  }

  private Usuario registrarDonante(Context ctx, String correo, String contrasena) {
    TipoDocumento tipoDocumento =
        enumerado(
            TipoDocumento.class, ctx.formParam("donanteTipoDocumento"), "el tipo de documento");
    String numeroDocumento =
        obligatorio(ctx, "donanteNumeroDocumento", "el número de documento").trim();
    boolean documentoUsado =
        donantes.getDonantes().stream()
            .anyMatch(
                d ->
                    d.getDocumento() == tipoDocumento
                        && d.getNumDocumento().equals(numeroDocumento));
    if (documentoUsado) {
      throw new DatoInvalido("Ya hay una persona donante registrada con ese documento.");
    }

    PersonaHumana persona =
        new PersonaHumana(
            new CorreoElectronico(correo),
            obligatorio(ctx, "donanteNombre", "el nombre"),
            obligatorio(ctx, "donanteApellido", "el apellido"),
            numeroDocumento,
            tipoDocumento,
            enumerado(Genero.class, ctx.formParam("donanteGenero"), "el género"),
            obligatorio(ctx, "donanteDireccion", "la dirección"));
    aplicarMediosDeContacto(persona, ctx, correo);

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

  // El correo siempre es un medio de contacto; el teléfono, si lo cargó, va por SMS o WhatsApp.
  private static void aplicarMediosDeContacto(PersonaHumana persona, Context ctx, String correo) {
    String telefono = ctx.formParam("donanteTelefono");
    String preferido = ctx.formParam("donanteMedioContacto");
    List<MedioDeContacto> medios = new ArrayList<>();
    medios.add(new CorreoElectronico(correo));
    boolean conTelefono = telefono != null && !telefono.isBlank();
    if (conTelefono) {
      medios.add("WHATSAPP".equals(preferido) ? new Whatsapp(telefono) : new Sms(telefono));
    } else if ("SMS".equals(preferido) || "WHATSAPP".equals(preferido)) {
      throw new DatoInvalido("Para recibir avisos por SMS o WhatsApp cargá un teléfono.");
    }
    boolean prefiereTelefono = "SMS".equals(preferido) || "WHATSAPP".equals(preferido);
    persona.reemplazarMediosDeContacto(medios, prefiereTelefono ? 1 : 0);
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

  private static String obligatorio(Context ctx, String campo, String nombre) {
    String valor = ctx.formParam(campo);
    if (valor == null || valor.isBlank()) {
      throw new DatoInvalido("Falta completar " + nombre + ".");
    }
    return valor;
  }

  private static <E extends Enum<E>> E enumerado(Class<E> tipo, String valor, String nombre) {
    try {
      return Enum.valueOf(tipo, valor == null ? "" : valor.trim());
    } catch (IllegalArgumentException e) {
      throw new DatoInvalido("Elegí " + nombre + ".");
    }
  }

  private Map<String, Object> modeloConError(Context ctx, String error) {
    Map<String, Object> modelo = new HashMap<>();
    authService.currentUser(ctx).ifPresent(usuario -> modelo.put("usuario", usuario));
    modelo.put("error", error);
    return modelo;
  }

  /** Un dato del formulario que no se puede aceptar; se muestra en la misma página. */
  private static final class DatoInvalido extends RuntimeException {
    DatoInvalido(String mensaje) {
      super(mensaje);
    }
  }
}
