package ar.edu.utn.donatrack.web.controller;

import static io.javalin.apibuilder.ApiBuilder.get;

import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.DonacionesRepository;
import ar.edu.utn.donatrack.donaciones.EstadoDonacion;
import ar.edu.utn.donatrack.donaciones.HistorialEstado;
import ar.edu.utn.donatrack.security.AuthService;
import io.javalin.http.Context;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pantallas accesibles sin iniciar sesión: la landing y las páginas públicas. El login y el panel
 * autenticado se manejan por AuthController con cookies JWT y sesión en memoria.
 */
public final class PaginasPublicasController {
  private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final AuthService authService;
  private final DonacionesRepository donaciones;

  public PaginasPublicasController(AuthService authService, DonacionesRepository donaciones) {
    this.authService = authService;
    this.donaciones = donaciones;
  }

  public void routes() {
    get("/", this::landing);
    get("/legal", this::legal);
    get("/entregas", this::entregas);
    get("/registro", this::registro);
  }

  private void landing(Context ctx) {
    ctx.render("index", modeloPublico(ctx));
  }

  private void legal(Context ctx) {
    ctx.render("legal", modeloPublico(ctx));
  }

  private void registro(Context ctx) {
    ctx.render("registro", modeloPublico(ctx));
  }

  /**
   * Donaciones entregadas con éxito, visibles sin iniciar sesión. Por la política de privacidad
   * solo se publica qué se entregó, a qué entidad, cuándo y las fotos que cargó: nunca datos de la
   * persona donante.
   */
  private void entregas(Context ctx) {
    List<EntregaPublica> entregas =
        donaciones.getDonaciones().stream()
            .filter(d -> d.getEstado() == EstadoDonacion.ENTREGADA)
            .filter(d -> d.getEntidadBeneficiaria() != null)
            .map(PaginasPublicasController::entregaPublica)
            .sorted(Comparator.comparing(EntregaPublica::orden).reversed())
            .toList();
    Map<String, Object> modelo = modeloPublico(ctx);
    modelo.put("entregas", entregas);
    ctx.render("entregas", modelo);
  }

  private static EntregaPublica entregaPublica(Donacion donacion) {
    LocalDateTime fecha =
        donacion.getHistorialEstado().stream()
            .filter(h -> h.getEstadoAlcanzado() == EstadoDonacion.ENTREGADA)
            .map(HistorialEstado::getFecha)
            .max(Comparator.naturalOrder())
            .orElse(LocalDateTime.MIN);
    return new EntregaPublica(
        donacion.getEntidadBeneficiaria().getRazonSocial(),
        fecha.equals(LocalDateTime.MIN) ? "" : fecha.format(FORMATO_FECHA),
        Vistas.bienes(donacion),
        donacion.getFotos().stream().filter(Vistas::esFotoMostrable).toList(),
        fecha);
  }

  /**
   * Lo que se muestra de cada entrega en la página pública. Tiene getters porque Handlebars lee las
   * propiedades como JavaBean.
   */
  public static final class EntregaPublica {
    private final String entidad;
    private final String fecha;
    private final List<String> bienes;
    private final List<String> fotos;
    private final LocalDateTime orden;

    EntregaPublica(
        String entidad,
        String fecha,
        List<String> bienes,
        List<String> fotos,
        LocalDateTime orden) {
      this.entidad = entidad;
      this.fecha = fecha;
      this.bienes = bienes;
      this.fotos = fotos;
      this.orden = orden;
    }

    public String getEntidad() {
      return entidad;
    }

    public String getFecha() {
      return fecha;
    }

    public List<String> getBienes() {
      return bienes;
    }

    public List<String> getFotos() {
      return fotos;
    }

    LocalDateTime orden() {
      return orden;
    }
  }

  private Map<String, Object> modeloPublico(Context ctx) {
    Map<String, Object> modelo = new HashMap<>();
    authService.currentUser(ctx).ifPresent(usuario -> modelo.put("usuario", usuario));
    return modelo;
  }
}
