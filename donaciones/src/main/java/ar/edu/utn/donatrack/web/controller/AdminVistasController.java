package ar.edu.utn.donatrack.web.controller;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

import ar.edu.utn.donatrack.beneficiarios.BeneficiariosRepository;
import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.DonacionesRepository;
import ar.edu.utn.donatrack.donaciones.EstadoDonacion;
import ar.edu.utn.donatrack.flota.Camion;
import ar.edu.utn.donatrack.flota.FlotaLogistica;
import ar.edu.utn.donatrack.importacion.Importacion;
import ar.edu.utn.donatrack.importacion.ImportadorDonantes;
import ar.edu.utn.donatrack.notificaciones.PublicadorDiferido;
import ar.edu.utn.donatrack.notificaciones.ServicioNotificaciones;
import ar.edu.utn.donatrack.propuestas.EntidadFueraDeRankingException;
import ar.edu.utn.donatrack.propuestas.PropuestaDeAsignacion;
import ar.edu.utn.donatrack.propuestas.PropuestaRepository;
import ar.edu.utn.donatrack.propuestas.ServicioMatchmakingDonaciones;
import ar.edu.utn.donatrack.security.AuthService;
import ar.edu.utn.donatrack.security.Role;
import ar.edu.utn.donatrack.security.Usuario;
import ar.edu.utn.donatrack.seguimiento.LogisticaNoDisponibleException;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.UploadedFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Vistas de la administración: vencidas, asignaciones, camiones e importación de donantes. */
public class AdminVistasController implements WithSimplePersistenceUnit {
  private static final Map<String, String> AVISOS =
      Map.ofEntries(
          Map.entry("vencida", "Marcamos la donación como vencida."),
          Map.entry("vencidas", "Marcamos como vencidas las donaciones con fecha pasada."),
          Map.entry("asignada", "Asignamos la donación y avisamos a la donante y a la entidad."),
          Map.entry("propuestas", "Generamos las propuestas para las donaciones en depósito."),
          Map.entry("camion", "Guardamos el camión."),
          Map.entry("eliminado", "Eliminamos el camión."));

  private final AuthService authService;
  private final DonacionesRepository donaciones;
  private final BeneficiariosRepository entidades;
  private final PropuestaRepository propuestas;
  private final ServicioMatchmakingDonaciones matchmaking;
  private final ServicioNotificaciones notificaciones;
  private final FlotaLogistica flota;
  private final ImportadorDonantes importador;
  private final Clock clock;

  AdminVistasController(
      AuthService authService,
      DonacionesRepository donaciones,
      BeneficiariosRepository entidades,
      PropuestaRepository propuestas,
      ServicioMatchmakingDonaciones matchmaking,
      ServicioNotificaciones notificaciones,
      FlotaLogistica flota,
      ImportadorDonantes importador,
      Clock clock) {
    this.authService = authService;
    this.donaciones = donaciones;
    this.entidades = entidades;
    this.propuestas = propuestas;
    this.matchmaking = matchmaking;
    this.notificaciones = notificaciones;
    this.flota = flota;
    this.importador = importador;
    this.clock = clock;
  }

  public void routes() {
    get("/admin/vencidas", this::vencidas);
    post("/admin/vencidas", this::marcarTodasVencidas);
    post("/admin/vencidas/{id}", this::marcarVencida);
    get("/admin/asignaciones", this::asignaciones);
    post("/admin/asignaciones", this::generarPropuestas);
    post("/admin/asignaciones/{id}", this::asignar);
    get("/admin/camiones", this::camiones);
    post("/admin/camiones", this::guardarCamion);
    post("/admin/camiones/{patente}/eliminar", this::eliminarCamion);
    get("/admin/importar", this::importaciones);
    post("/admin/importar", this::importar);
  }

  // --- Donaciones vencidas en depósito (#57) ---

  private void vencidas(Context ctx) {
    Usuario usuario = Vistas.exigir(ctx, authService, Role.ADMIN);
    LocalDate hoy = LocalDate.now(clock);
    List<DepositoVista> enDeposito =
        donaciones.getDonacionesEnEstado(EstadoDonacion.EN_DEPOSITO).stream()
            .map(d -> new DepositoVista(d, hoy))
            .sorted(
                Comparator.comparing(DepositoVista::isVencida)
                    .reversed()
                    .thenComparing(DepositoVista::orden))
            .toList();
    Map<String, Object> modelo = Vistas.modelo(usuario);
    modelo.put("donaciones", enDeposito);
    modelo.put("vencidas", enDeposito.stream().filter(DepositoVista::isVencida).count());
    aviso(ctx, modelo);
    ctx.render("admin-vencidas", modelo);
  }

  private void marcarVencida(Context ctx) {
    Vistas.exigir(ctx, authService, Role.ADMIN);
    Donacion donacion = donacion(ctx);
    if (donacion.getEstado() != EstadoDonacion.EN_DEPOSITO) {
      throw new IllegalStateException("La donación ya no está en el depósito.");
    }
    vencer(List.of(donacion));
    ctx.redirect("/admin/vencidas?ok=vencida");
  }

  private void marcarTodasVencidas(Context ctx) {
    Vistas.exigir(ctx, authService, Role.ADMIN);
    LocalDate hoy = LocalDate.now(clock);
    vencer(
        donaciones.getDonacionesEnEstado(EstadoDonacion.EN_DEPOSITO).stream()
            .filter(d -> new DepositoVista(d, hoy).isVencida())
            .toList());
    ctx.redirect("/admin/vencidas?ok=vencidas");
  }

  private void vencer(List<Donacion> vencidas) {
    withTransaction(
        () ->
            vencidas.forEach(
                d -> {
                  d.marcarVencida();
                  propuestas.remove(d);
                  donaciones.save(d);
                }));
  }

  // --- Asignación de entidad final desde las propuestas (#58) ---

  private void asignaciones(Context ctx) {
    Usuario usuario = Vistas.exigir(ctx, authService, Role.ADMIN);
    Map<String, Object> modelo = Vistas.modelo(usuario);
    modelo.put("propuestas", propuestasVista(null, null));
    modelo.put(
        "sinPropuesta",
        donaciones.getDonacionesEnEstado(EstadoDonacion.EN_DEPOSITO).stream()
            .filter(d -> propuestas.findPendingByDonacion(d).isEmpty())
            .count());
    aviso(ctx, modelo);
    ctx.render("admin-asignaciones", modelo);
  }

  private void generarPropuestas(Context ctx) {
    Vistas.exigir(ctx, authService, Role.ADMIN);
    withTransaction(() -> matchmaking.ejecutarMatchmakingTotal(LocalDate.now(clock)));
    ctx.redirect("/admin/asignaciones?ok=propuestas");
  }

  private void asignar(Context ctx) {
    Usuario usuario = Vistas.exigir(ctx, authService, Role.ADMIN);
    Donacion donacion = donacion(ctx);
    Optional<EntidadBeneficiaria> entidad =
        Optional.ofNullable(ctx.formParam("entidad"))
            .flatMap(AdminVistasController::numero)
            .flatMap(entidades::findById);
    PublicadorDiferido publicador = notificaciones.publicadorDiferido();
    try {
      EntidadBeneficiaria elegida =
          entidad.orElseThrow(() -> new IllegalArgumentException("Elegí una entidad."));
      withTransaction(
          () ->
              matchmaking.confirmarAsignacionAdministrativa(
                  donacion, elegida, LocalDate.now(clock), publicador));
    } catch (IllegalArgumentException | IllegalStateException e) {
      String mensaje =
          e instanceof EntidadFueraDeRankingException
              ? "Esa entidad no está entre las propuestas para esta donación."
              : e.getMessage();
      Map<String, Object> modelo = Vistas.modelo(usuario);
      modelo.put("propuestas", propuestasVista(donacion.getId(), mensaje));
      ctx.status(400).render("admin-asignaciones", modelo);
      return;
    }
    publicador.enviarPendientes();
    ctx.redirect("/admin/asignaciones?ok=asignada");
  }

  private List<PropuestaVista> propuestasVista(UUID conError, String error) {
    return propuestas.findAllPending().stream()
        .map(
            p ->
                donaciones
                    .findById(p.getDonacion().getId())
                    .filter(d -> d.getEstado() == EstadoDonacion.EN_DEPOSITO)
                    .map(d -> new PropuestaVista(d, p, d.getId().equals(conError) ? error : null)))
        .flatMap(Optional::stream)
        .toList();
  }

  // --- Administración de camiones (#59), operada en logística por GraphQL ---

  private void camiones(Context ctx) {
    Usuario usuario = Vistas.exigir(ctx, authService, Role.ADMIN);
    Map<String, Object> modelo = Vistas.modelo(usuario);
    aviso(ctx, modelo);
    renderCamiones(ctx, modelo);
  }

  private void guardarCamion(Context ctx) {
    Usuario usuario = Vistas.exigir(ctx, authService, Role.ADMIN);
    String patente = Optional.ofNullable(ctx.formParam("patente")).orElse("").trim().toUpperCase();
    try {
      if (!patente.matches("[A-Z0-9]{6,7}")) {
        throw new IllegalArgumentException("La patente tiene que tener 6 o 7 letras y números.");
      }
      Camion camion =
          new Camion(
              patente,
              positivo(ctx.formParam("volumen"), "el volumen"),
              positivo(ctx.formParam("altura"), "la altura"),
              positivo(ctx.formParam("capacidad"), "la capacidad de carga"));
      if ("editar".equals(ctx.formParam("modo"))) {
        flota.actualizar(sesion(ctx), camion);
      } else {
        flota.crear(sesion(ctx), camion);
      }
    } catch (IllegalArgumentException | LogisticaNoDisponibleException e) {
      Map<String, Object> modelo = Vistas.modelo(usuario);
      modelo.put("error", e.getMessage());
      modelo.put("form", Map.of("patente", patente));
      ctx.status(400);
      renderCamiones(ctx, modelo);
      return;
    }
    ctx.redirect("/admin/camiones?ok=camion");
  }

  private void eliminarCamion(Context ctx) {
    Usuario usuario = Vistas.exigir(ctx, authService, Role.ADMIN);
    try {
      flota.eliminar(sesion(ctx), ctx.pathParam("patente"));
    } catch (IllegalArgumentException | LogisticaNoDisponibleException e) {
      Map<String, Object> modelo = Vistas.modelo(usuario);
      modelo.put("error", e.getMessage());
      ctx.status(400);
      renderCamiones(ctx, modelo);
      return;
    }
    ctx.redirect("/admin/camiones?ok=eliminado");
  }

  private void renderCamiones(Context ctx, Map<String, Object> modelo) {
    try {
      modelo.put(
          "camiones",
          flota.camiones(sesion(ctx)).stream()
              .sorted(Comparator.comparing(Camion::getPatente))
              .toList());
    } catch (IllegalArgumentException | LogisticaNoDisponibleException e) {
      modelo.put("camiones", List.of());
      modelo.putIfAbsent("error", "No pudimos consultar la flota a logística: " + e.getMessage());
    }
    ctx.render("admin-camiones", modelo);
  }

  private static String sesion(Context ctx) {
    return Optional.ofNullable(ctx.cookie("access_token")).orElse("");
  }

  // --- Importación de donantes desde CSV (#60) ---

  private void importaciones(Context ctx) {
    Usuario usuario = Vistas.exigir(ctx, authService, Role.ADMIN);
    Map<String, Object> modelo = Vistas.modelo(usuario);
    List<Importacion> recientes = importador.recientes();
    modelo.put("importaciones", recientes);
    modelo.put("enCurso", recientes.stream().anyMatch(Importacion::isEnCurso));
    ctx.render("admin-importar", modelo);
  }

  private void importar(Context ctx) {
    Usuario usuario = Vistas.exigir(ctx, authService, Role.ADMIN);
    UploadedFile archivo = ctx.uploadedFile("archivo");
    if (archivo == null
        || archivo.size() == 0
        || !archivo.filename().toLowerCase().endsWith(".csv")) {
      Map<String, Object> modelo = Vistas.modelo(usuario);
      modelo.put("importaciones", importador.recientes());
      modelo.put("error", "Elegí un archivo .csv.");
      ctx.status(400).render("admin-importar", modelo);
      return;
    }
    // El archivo se copia a disco: el pedido HTTP termina antes que la importación.
    Path temporal;
    try (InputStream contenido = archivo.content()) {
      temporal = Files.createTempFile("donatrack-importacion-", ".csv");
      Files.copy(contenido, temporal, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    importador.iniciar(archivo.filename(), temporal);
    ctx.redirect("/admin/importar");
  }

  // --- Comunes ---

  private Donacion donacion(Context ctx) {
    try {
      return donaciones
          .findById(UUID.fromString(ctx.pathParam("id")))
          .orElseThrow(NotFoundResponse::new);
    } catch (IllegalArgumentException e) {
      throw new NotFoundResponse();
    }
  }

  private static void aviso(Context ctx, Map<String, Object> modelo) {
    Optional.ofNullable(ctx.queryParam("ok"))
        .map(AVISOS::get)
        .ifPresent(aviso -> modelo.put("flash", aviso));
  }

  private static Optional<Long> numero(String valor) {
    try {
      return Optional.of(Long.parseLong(valor));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }

  private static double positivo(String valor, String campo) {
    try {
      double numero = Double.parseDouble(Optional.ofNullable(valor).orElse("").replace(',', '.'));
      if (!Double.isFinite(numero) || numero <= 0) {
        throw new NumberFormatException();
      }
      return numero;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Indicá " + campo + " con un número mayor a cero.");
    }
  }

  /** Una donación en depósito (getters para Handlebars). */
  public static final class DepositoVista {
    private final Donacion donacion;
    private final LocalDate vence;
    private final boolean vencida;

    DepositoVista(Donacion donacion, LocalDate hoy) {
      this.donacion = donacion;
      this.vence = donacion.getBienes().isEmpty() ? null : donacion.getFechaVencimiento();
      this.vencida = vence != null && vence.isBefore(hoy);
    }

    LocalDate orden() {
      return vence == null ? LocalDate.MAX : vence;
    }

    public String getId() {
      return donacion.getId().toString();
    }

    public List<String> getBienes() {
      return Vistas.bienes(donacion);
    }

    public String getVence() {
      return vence == null ? "" : Vistas.FECHA.format(vence);
    }

    public boolean isVencida() {
      return vencida;
    }
  }

  /** Una propuesta pendiente con sus entidades candidatas (getters para Handlebars). */
  public static final class PropuestaVista {
    private final Donacion donacion;
    private final List<Candidato> candidatos;
    private final String error;

    PropuestaVista(Donacion donacion, PropuestaDeAsignacion propuesta, String error) {
      this.donacion = donacion;
      this.error = error;
      List<Candidato> lista = new ArrayList<>();
      propuesta
          .getCandidatos()
          .forEach(
              c ->
                  lista.add(
                      new Candidato(
                          c.getEntidadBeneficiaria().getId(),
                          c.getEntidadBeneficiaria().getRazonSocial(),
                          c.getPuntaje(),
                          lista.isEmpty())));
      this.candidatos = lista;
    }

    public String getId() {
      return donacion.getId().toString();
    }

    public List<String> getBienes() {
      return Vistas.bienes(donacion);
    }

    public List<Candidato> getCandidatos() {
      return candidatos;
    }

    public String getError() {
      return error;
    }
  }

  /** Una entidad candidata. La primera es la mejor puntuada y queda elegida por defecto. */
  public static final class Candidato {
    private final Long id;
    private final String nombre;
    private final int puntaje;
    private final boolean primera;

    Candidato(Long id, String nombre, int puntaje, boolean primera) {
      this.id = id;
      this.nombre = nombre;
      this.puntaje = puntaje;
      this.primera = primera;
    }

    public Long getId() {
      return id;
    }

    public String getNombre() {
      return nombre;
    }

    public int getPuntaje() {
      return puntaje;
    }

    public boolean isPrimera() {
      return primera;
    }
  }
}
