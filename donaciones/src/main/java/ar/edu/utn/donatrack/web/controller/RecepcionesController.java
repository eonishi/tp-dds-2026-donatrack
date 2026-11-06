package ar.edu.utn.donatrack.web.controller;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.DonacionesRepository;
import ar.edu.utn.donatrack.donaciones.EstadoDonacion;
import ar.edu.utn.donatrack.fotos.FotosRecepcion;
import ar.edu.utn.donatrack.fotos.FotosRecepcion.FotoSubida;
import ar.edu.utn.donatrack.notificaciones.Notificacion;
import ar.edu.utn.donatrack.notificaciones.ServicioNotificaciones;
import ar.edu.utn.donatrack.security.AuthService;
import ar.edu.utn.donatrack.security.Role;
import ar.edu.utn.donatrack.security.Usuario;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.UploadedFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Vista de la entidad beneficiaria para confirmar la recepción de sus donaciones con fotos, o
 * avisar que llegó con problemas.
 */
public class RecepcionesController implements WithSimplePersistenceUnit {
  private static final Logger LOG = Logger.getLogger(RecepcionesController.class.getName());
  private static final Map<String, String> AVISOS =
      Map.of(
          "confirmada", "¡Listo! Registramos la recepción y avisamos a la persona donante.",
          "reportada", "Registramos el problema con la entrega. El equipo de DonaTrack lo revisa.");

  private final AuthService authService;
  private final DonacionesRepository donaciones;
  private final FotosRecepcion fotos;
  private final ServicioNotificaciones notificaciones;

  public RecepcionesController(
      AuthService authService,
      DonacionesRepository donaciones,
      FotosRecepcion fotos,
      ServicioNotificaciones notificaciones) {
    this.authService = authService;
    this.donaciones = donaciones;
    this.fotos = fotos;
    this.notificaciones = notificaciones;
  }

  public void routes() {
    get("/entidad/recepciones", this::listar);
    post("/entidad/recepciones/{id}/confirmar", this::confirmar);
    post("/entidad/recepciones/{id}/reportar", this::reportar);
  }

  private void listar(Context ctx) {
    Usuario usuario = Vistas.exigir(ctx, authService, Role.BENEFICIARIO);
    Map<String, Object> modelo = modelo(usuario, null, null);
    Optional.ofNullable(ctx.queryParam("ok"))
        .map(AVISOS::get)
        .ifPresent(aviso -> modelo.put("flash", aviso));
    ctx.render("entidad-recepciones", modelo);
  }

  private void confirmar(Context ctx) {
    Usuario usuario = Vistas.exigir(ctx, authService, Role.BENEFICIARIO);
    Donacion donacion = donacionPropia(ctx, usuario);
    List<Notificacion> generadas = new ArrayList<>();
    try {
      exigirEnCamino(donacion);
      List<String> urls = fotos.guardar(donacion.getId(), fotosSubidas(ctx));
      withTransaction(
          () -> {
            urls.forEach(donacion::agregarFoto);
            generadas.addAll(donacion.confirmarEntrega());
            donaciones.save(donacion);
          });
    } catch (IllegalArgumentException | IllegalStateException e) {
      conError(ctx, usuario, donacion, e.getMessage());
      return;
    } catch (IOException e) {
      LOG.log(Level.WARNING, "No se pudieron guardar las fotos de " + donacion.getId(), e);
      conError(ctx, usuario, donacion, "No pudimos guardar las fotos. Probá de nuevo en un rato.");
      return;
    }
    notificaciones.enviar(generadas);
    ctx.redirect("/entidad/recepciones?ok=confirmada");
  }

  private void reportar(Context ctx) {
    Usuario usuario = Vistas.exigir(ctx, authService, Role.BENEFICIARIO);
    Donacion donacion = donacionPropia(ctx, usuario);
    String motivo = Optional.ofNullable(ctx.formParam("motivo")).map(String::trim).orElse("");
    List<Notificacion> generadas = new ArrayList<>();
    try {
      exigirEnCamino(donacion);
      if (motivo.isEmpty()) {
        throw new IllegalArgumentException("Contanos qué pasó con la entrega.");
      }
      withTransaction(
          () -> {
            generadas.addAll(donacion.reportarFalla(motivo));
            donaciones.save(donacion);
          });
    } catch (IllegalArgumentException | IllegalStateException e) {
      conError(ctx, usuario, donacion, e.getMessage());
      return;
    }
    notificaciones.enviar(generadas);
    ctx.redirect("/entidad/recepciones?ok=reportada");
  }

  private static void exigirEnCamino(Donacion donacion) {
    if (donacion.getEstado() != EstadoDonacion.EN_TRASLADO) {
      throw new IllegalStateException("Esta donación ya no está en camino.");
    }
  }

  /** La donación pedida, solo si está asignada a la entidad de la sesión. */
  private Donacion donacionPropia(Context ctx, Usuario usuario) {
    UUID id;
    try {
      id = UUID.fromString(ctx.pathParam("id"));
    } catch (IllegalArgumentException e) {
      throw new NotFoundResponse();
    }
    Long entidadId = usuario.getEntidadId().orElse(null);
    return donaciones
        .findById(id)
        .filter(d -> d.getEntidadBeneficiaria() != null)
        .filter(d -> d.getEntidadBeneficiaria().getId().equals(entidadId))
        .orElseThrow(NotFoundResponse::new);
  }

  private static List<FotoSubida> fotosSubidas(Context ctx) {
    List<FotoSubida> subidas = new ArrayList<>();
    for (UploadedFile archivo : ctx.uploadedFiles("fotos")) {
      if (archivo.size() == 0) {
        continue;
      }
      if (archivo.size() > FotosRecepcion.TAMANIO_MAXIMO) {
        throw new IllegalArgumentException("La foto " + archivo.filename() + " pesa más de 5 MB.");
      }
      try (InputStream contenido = archivo.content()) {
        subidas.add(new FotoSubida(archivo.filename(), contenido.readAllBytes()));
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    return subidas;
  }

  private void conError(Context ctx, Usuario usuario, Donacion donacion, String mensaje) {
    Map<String, Object> modelo = modelo(usuario, donacion.getId(), mensaje);
    ctx.status(400).render("entidad-recepciones", modelo);
  }

  /** El error, si lo hay, se muestra en la tarjeta de la donación que lo causó. */
  private Map<String, Object> modelo(Usuario usuario, UUID conError, String error) {
    Map<String, Object> modelo = Vistas.modelo(usuario);
    List<Donacion> propias =
        usuario.getEntidadId().map(donaciones::getDonacionesDeEntidad).orElse(List.of()).stream()
            .sorted(Comparator.comparing(Vistas::ultimoCambio).reversed())
            .toList();
    modelo.put(
        "enCamino",
        propias.stream()
            .filter(d -> d.getEstado() == EstadoDonacion.EN_TRASLADO)
            .map(d -> new RecepcionVista(d, d.getId().equals(conError) ? error : null))
            .toList());
    modelo.put(
        "recibidas",
        propias.stream()
            .filter(d -> d.getEstado() == EstadoDonacion.ENTREGADA)
            .map(d -> new RecepcionVista(d, null))
            .toList());
    modelo.put("maximoFotos", FotosRecepcion.MAXIMO_FOTOS);
    return modelo;
  }

  /** Lo que muestra la vista de cada donación (getters para Handlebars). */
  public static final class RecepcionVista {
    private final Donacion donacion;
    private final String error;

    RecepcionVista(Donacion donacion, String error) {
      this.donacion = donacion;
      this.error = error;
    }

    public String getError() {
      return error;
    }

    public String getId() {
      return donacion.getId().toString();
    }

    public List<String> getBienes() {
      return Vistas.bienes(donacion);
    }

    public String getCamion() {
      return donacion.getIdCamion().orElse("");
    }

    public String getSalio() {
      return Vistas.fechaDe(donacion, EstadoDonacion.EN_TRASLADO)
          .map(Vistas.FECHA_HORA::format)
          .orElse("");
    }

    public String getRecibida() {
      return Vistas.fechaDe(donacion, EstadoDonacion.ENTREGADA)
          .map(Vistas.FECHA_HORA::format)
          .orElse("");
    }

    public List<String> getFotos() {
      return donacion.getFotos().stream().filter(Vistas::esFotoMostrable).toList();
    }
  }
}
