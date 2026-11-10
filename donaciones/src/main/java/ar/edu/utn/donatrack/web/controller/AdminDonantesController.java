package ar.edu.utn.donatrack.web.controller;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

import ar.edu.utn.donatrack.donaciones.DonacionesRepository;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.DonantesRepository;
import ar.edu.utn.donatrack.donantes.MedioDeContacto;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.PersonajeJuridica;
import ar.edu.utn.donatrack.donantes.Sms;
import ar.edu.utn.donatrack.donantes.Whatsapp;
import ar.edu.utn.donatrack.notificaciones.BandejaNotificacionesRepository;
import ar.edu.utn.donatrack.notificaciones.ServicioNotificaciones;
import ar.edu.utn.donatrack.notificaciones.eventos.EventoBienvenida;
import ar.edu.utn.donatrack.security.AuthService;
import ar.edu.utn.donatrack.security.Role;
import ar.edu.utn.donatrack.security.Usuario;
import ar.edu.utn.donatrack.web.controller.Formularios.DatoInvalido;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Administración de personas donantes (entrega 4, punto 15): listado paginado con búsqueda —pensado
 * para los miles que llegan por CSV—, alta manual de una persona donante recibida en el depósito, y
 * edición y baja.
 *
 * <p>Quien se da de alta acá recibe una cuenta pendiente de activación, igual que los importados, y
 * un correo de bienvenida con el enlace a {@code /activar} para entrar por primera vez (entrega 1,
 * registro en el depósito): ahí confirma su documento y elige su contraseña, así la administración
 * nunca la conoce.
 */
public final class AdminDonantesController implements WithSimplePersistenceUnit {
  static final int POR_PAGINA = 25;

  private final AuthService authService;
  private final DonantesRepository donantes;
  private final DonacionesRepository donaciones;
  private final BandejaNotificacionesRepository bandeja;
  private final ServicioNotificaciones notificaciones;

  public AdminDonantesController(
      AuthService authService,
      DonantesRepository donantes,
      DonacionesRepository donaciones,
      BandejaNotificacionesRepository bandeja,
      ServicioNotificaciones notificaciones) {
    this.authService = authService;
    this.donantes = donantes;
    this.donaciones = donaciones;
    this.bandeja = bandeja;
    this.notificaciones = notificaciones;
  }

  public void routes() {
    get("/admin/donantes", this::listar, Role.ADMIN);
    post("/admin/donantes", this::crear, Role.ADMIN);
    get("/admin/donantes/{id}", this::editar, Role.ADMIN);
    post("/admin/donantes/{id}", this::actualizar, Role.ADMIN);
    post("/admin/donantes/{id}/eliminar", this::eliminar, Role.ADMIN);
  }

  // Lo que se le confirma a la administración al volver al listado (?ok=...).
  private static final Map<String, String> AVISOS =
      Map.of(
          "creado",
          "Registramos a la persona donante y le enviamos la bienvenida con el enlace para activar"
              + " su cuenta (correo y documento).",
          "editado",
          "Guardamos los cambios de la persona donante.",
          "eliminado",
          "Eliminamos a la persona donante y su cuenta.");

  private void listar(Context ctx) {
    Usuario usuario = Vistas.usuario(ctx);
    Map<String, Object> modelo = Vistas.modelo(usuario);
    Optional.ofNullable(ctx.queryParam("ok"))
        .map(AVISOS::get)
        .ifPresent(aviso -> modelo.put("flash", aviso));
    ctx.render("admin-donantes", completar(modelo, ctx.queryParam("q"), ctx.queryParam("pagina")));
  }

  private void crear(Context ctx) {
    Usuario usuario = Vistas.usuario(ctx);
    try {
      String correo = Formularios.correo(ctx, "correo");
      if (authService.existeUsuario(correo) || donantes.existeCorreo(correo)) {
        throw new DatoInvalido("Ya hay una persona donante o una cuenta con ese correo.");
      }
      Donante donante = FormularioDonante.armar(ctx, correo, donantes);
      Donante guardado =
          withTransaction(
              () -> {
                Donante nuevo = donantes.save(donante);
                authService.agregarPendiente(correo, nuevo.getId());
                return nuevo;
              });
      // Fuera de la transacción: si el envío falla, el alta igual queda hecha.
      String enlace =
          Vistas.urlPublica(
              ctx, "/activar?correo=" + URLEncoder.encode(correo, StandardCharsets.UTF_8));
      notificaciones.enviar(new EventoBienvenida(guardado, enlace).generarNotificaciones());
      volverAlListado(ctx, "creado", donante.getNumDocumento());
    } catch (DatoInvalido e) {
      Map<String, Object> modelo = Vistas.modelo(usuario);
      modelo.put("error", e.getMessage());
      modelo.put("form", valoresDelFormulario(ctx));
      modelo.put("formAbierto", true);
      ctx.status(400).render("admin-donantes", completar(modelo, null, null));
    }
  }

  private void editar(Context ctx) {
    Donante donante = buscar(ctx);
    mostrarEdicion(ctx, donante, FormularioDonante.valores(donante), null);
  }

  private void actualizar(Context ctx) {
    Donante actual = buscar(ctx);
    try {
      String correo = Formularios.correo(ctx, "correo");
      boolean cambiaCorreo = !correo.equalsIgnoreCase(actual.extraerEmail().orElse(""));
      if (cambiaCorreo && (authService.existeUsuario(correo) || donantes.existeCorreo(correo))) {
        throw new DatoInvalido("Ya hay una persona donante o una cuenta con ese correo.");
      }
      // Valida todo antes de tocar al donante: si algo no se acepta, no cambia nada.
      FormularioDonante.aplicar(actual, ctx, correo, donantes);
      withTransaction(
          () -> {
            donantes.save(actual);
            if (cambiaCorreo) {
              // El correo es también el usuario: la persona pasa a entrar con el nuevo.
              authService.cambiarCorreoDeDonante(actual.getId(), correo);
            }
          });
      volverAlListado(ctx, "editado", actual.getNumDocumento());
    } catch (DatoInvalido e) {
      ctx.status(400);
      mostrarEdicion(ctx, actual, valoresDelFormulario(ctx), e.getMessage());
    }
  }

  private void eliminar(Context ctx) {
    Donante donante = buscar(ctx);
    // Sus donaciones son historia de las entidades y del mapa de entregas: no se borran.
    if (donaciones.tieneDonacionesDe(donante.getId())) {
      ctx.status(409);
      mostrarEdicion(
          ctx,
          donante,
          FormularioDonante.valores(donante),
          "No se puede eliminar a una persona donante con donaciones registradas.");
      return;
    }
    withTransaction(
        () -> {
          authService.eliminarCuentaDeDonante(donante.getId());
          bandeja.borrarDeDonante(donante.getId());
          donantes.deleteById(donante.getId());
        });
    volverAlListado(ctx, "eliminado", "");
  }

  private Donante buscar(Context ctx) {
    UUID id;
    try {
      id = UUID.fromString(ctx.pathParam("id"));
    } catch (IllegalArgumentException e) {
      throw new NotFoundResponse("No existe esa persona donante.");
    }
    return donantes
        .findById(id)
        .orElseThrow(() -> new NotFoundResponse("No existe esa persona donante."));
  }

  private void mostrarEdicion(
      Context ctx, Donante donante, Map<String, String> valores, String error) {
    Map<String, Object> modelo = Vistas.modelo(Vistas.usuario(ctx));
    modelo.put("id", donante.getId());
    modelo.put("nombre", fila(donante, null).nombre());
    modelo.put("form", valores);
    modelo.put("editando", true);
    if (error != null) {
      modelo.put("error", error);
    }
    ctx.render("admin-donante", modelo);
  }

  private static void volverAlListado(Context ctx, String aviso, String busqueda) {
    ctx.redirect(
        "/admin/donantes?ok="
            + aviso
            + "&q="
            + URLEncoder.encode(busqueda, StandardCharsets.UTF_8));
  }

  private Map<String, Object> completar(
      Map<String, Object> modelo, String texto, String paginaPedida) {
    String busqueda = texto == null ? "" : texto.trim();
    int desde =
        Vistas.paginar(
            modelo,
            "/admin/donantes",
            busqueda,
            donantes.contar(busqueda),
            paginaPedida,
            POR_PAGINA);

    List<Donante> encontrados = donantes.buscar(busqueda, desde, POR_PAGINA);
    Map<UUID, Usuario> cuentas =
        authService.cuentasDeDonantes(encontrados.stream().map(Donante::getId).toList());
    modelo.put("donantes", encontrados.stream().map(d -> fila(d, cuentas.get(d.getId()))).toList());
    return modelo;
  }

  /** Lo que se muestra de cada donante en la tabla. */
  public record Fila(
      String editar,
      String tipo,
      String nombre,
      String documento,
      String correo,
      String telefono,
      String cuenta,
      String cuentaClase) {}

  private static Fila fila(Donante donante, Usuario cuenta) {
    String tipo;
    String nombre;
    if (donante instanceof PersonajeJuridica juridica) {
      tipo = "Jurídica";
      nombre = juridica.getRazonSocial();
    } else if (donante instanceof PersonaHumana humana) {
      tipo = "Humana";
      nombre =
          (humana.getNombre() + " " + Optional.ofNullable(humana.getApellido()).orElse("")).trim();
    } else {
      tipo = "";
      nombre = "";
    }
    String telefono =
        donante.getMediosDeContacto().stream()
            .filter(AdminDonantesController::esTelefono)
            .map(m -> (m instanceof Whatsapp ? "WhatsApp " : "SMS ") + m.getContacto())
            .findFirst()
            .orElse("");
    String estado;
    String clase;
    if (cuenta == null) {
      estado = "Sin cuenta";
      clase = "text-bg-secondary";
    } else if (cuenta.isPendienteDeActivacion()) {
      estado = "Pendiente de activación";
      clase = "text-bg-warning";
    } else {
      estado = "Activa";
      clase = "text-bg-success";
    }
    return new Fila(
        "/admin/donantes/" + donante.getId(),
        tipo,
        nombre,
        donante.getDocumento() + " " + donante.getNumDocumento(),
        donante.extraerEmail().orElse(""),
        telefono,
        estado,
        clase);
  }

  private static Map<String, String> valoresDelFormulario(Context ctx) {
    Map<String, String> valores = new HashMap<>();
    ctx.formParamMap()
        .forEach(
            (campo, lista) -> {
              if (!lista.isEmpty()) {
                valores.put(campo, lista.get(0));
              }
            });
    return valores;
  }

  private static boolean esTelefono(MedioDeContacto medio) {
    return medio instanceof Sms || medio instanceof Whatsapp;
  }
}
