package ar.edu.utn.donatrack.web.controller;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

import ar.edu.utn.donatrack.beneficiarios.BeneficiariosRepository;
import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.beneficiarios.Necesidad;
import ar.edu.utn.donatrack.beneficiarios.NecesidadRecurrente;
import ar.edu.utn.donatrack.beneficiarios.TipoPeriodo;
import ar.edu.utn.donatrack.bienes.Categoria;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.bienes.SubcategoriasRepository;
import ar.edu.utn.donatrack.bienes.UnidadMedida;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.DonacionesRepository;
import ar.edu.utn.donatrack.notificaciones.BandejaNotificacionesRepository;
import ar.edu.utn.donatrack.security.AuthService;
import ar.edu.utn.donatrack.security.Role;
import ar.edu.utn.donatrack.security.Usuario;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.NotFoundResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/** Vistas de la entidad beneficiaria: sus necesidades, el seguimiento y sus avisos. */
public class EntidadVistasController implements WithSimplePersistenceUnit {
  private static final Map<String, String> AVISOS =
      Map.of(
          "agregada", "Agregamos la necesidad. La vamos a tener en cuenta al asignar donaciones.",
          "eliminada", "Eliminamos la necesidad.");

  private final AuthService authService;
  private final BeneficiariosRepository entidades;
  private final SubcategoriasRepository subcategorias;
  private final DonacionesRepository donaciones;
  private final BandejaNotificacionesRepository bandeja;
  private final Seguimiento seguimiento;

  EntidadVistasController(
      AuthService authService,
      BeneficiariosRepository entidades,
      SubcategoriasRepository subcategorias,
      DonacionesRepository donaciones,
      BandejaNotificacionesRepository bandeja,
      Seguimiento seguimiento) {
    this.authService = authService;
    this.entidades = entidades;
    this.subcategorias = subcategorias;
    this.donaciones = donaciones;
    this.bandeja = bandeja;
    this.seguimiento = seguimiento;
  }

  public void routes() {
    get("/entidad/necesidades", this::necesidades);
    post("/entidad/necesidades", this::agregarNecesidad);
    post("/entidad/necesidades/{id}/eliminar", this::eliminarNecesidad);
    get("/entidad/seguimiento", this::seguimiento);
    get("/entidad/seguimiento/posiciones", this::posiciones);
    get("/entidad/notificaciones", this::notificaciones);
  }

  private void necesidades(Context ctx) {
    Usuario usuario = Vistas.exigir(ctx, authService, Role.BENEFICIARIO);
    EntidadBeneficiaria entidad = propia(usuario);
    Map<String, Object> modelo = modeloNecesidades(usuario, entidad, Map.of());
    Optional.ofNullable(ctx.queryParam("ok"))
        .map(AVISOS::get)
        .ifPresent(aviso -> modelo.put("flash", aviso));
    ctx.render("entidad-necesidades", modelo);
  }

  private void agregarNecesidad(Context ctx) {
    Usuario usuario = Vistas.exigir(ctx, authService, Role.BENEFICIARIO);
    EntidadBeneficiaria entidad = propia(usuario);
    Map<String, String> datos = new HashMap<>();
    for (String campo :
        List.of(
            "tipo", "periodo", "categoria", "subcategoria", "cantidad", "unidad", "descripcion")) {
      datos.put(campo, Optional.ofNullable(ctx.formParam(campo)).map(String::trim).orElse(""));
    }
    try {
      Categoria categoria = enumerado(Categoria.class, datos.get("categoria"), "la categoría");
      UnidadMedida unidad = enumerado(UnidadMedida.class, datos.get("unidad"), "la unidad");
      String nombre = obligatorio(datos.get("subcategoria"), "Indicá qué necesitás.");
      String descripcion = obligatorio(datos.get("descripcion"), "Agregá una descripción.");
      double cantidad = cantidad(datos.get("cantidad"));
      boolean recurrente = "RECURRENTE".equals(datos.get("tipo"));
      TipoPeriodo periodo =
          recurrente ? enumerado(TipoPeriodo.class, datos.get("periodo"), "cada cuánto") : null;
      withTransaction(
          () -> {
            SubCategoria subcategoria =
                subcategorias.obtenerOrCrear(nombre, categoria, new HashMap<>());
            if (recurrente) {
              NecesidadRecurrente necesidad =
                  new NecesidadRecurrente(subcategoria, cantidad, unidad, descripcion);
              necesidad.setPeriodo(periodo);
              entidad.agregarNecesidadRecurrente(necesidad);
            } else {
              entidad.agregarNecesidadExtraordinaria(
                  new Necesidad(subcategoria, cantidad, unidad, descripcion));
            }
            entidades.save(entidad);
          });
    } catch (IllegalArgumentException | IllegalStateException e) {
      Map<String, Object> modelo = modeloNecesidades(usuario, entidad, datos);
      modelo.put("error", e.getMessage());
      ctx.status(400).render("entidad-necesidades", modelo);
      return;
    }
    ctx.redirect("/entidad/necesidades?ok=agregada");
  }

  private void eliminarNecesidad(Context ctx) {
    Usuario usuario = Vistas.exigir(ctx, authService, Role.BENEFICIARIO);
    EntidadBeneficiaria entidad = propia(usuario);
    UUID id;
    try {
      id = UUID.fromString(ctx.pathParam("id"));
    } catch (IllegalArgumentException e) {
      throw new NotFoundResponse();
    }
    if (entidad.buscarNecesidad(id).isEmpty()) {
      throw new NotFoundResponse();
    }
    withTransaction(() -> entidad.eliminarNecesidad(id));
    ctx.redirect("/entidad/necesidades?ok=eliminada");
  }

  private void seguimiento(Context ctx) {
    Usuario usuario = Vistas.exigir(ctx, authService, Role.BENEFICIARIO);
    Map<String, Object> modelo = Vistas.modelo(usuario);
    modelo.put("entregas", seguimiento.entregas(asignadas(usuario)));
    modelo.put("posiciones", "/entidad/seguimiento/posiciones");
    modelo.put("vistaEntidad", true);
    ctx.render("seguimiento", modelo);
  }

  private void posiciones(Context ctx) {
    Usuario usuario = Vistas.exigir(ctx, authService, Role.BENEFICIARIO);
    ctx.json(seguimiento.entregas(asignadas(usuario)));
  }

  private void notificaciones(Context ctx) {
    Usuario usuario = Vistas.exigir(ctx, authService, Role.BENEFICIARIO);
    Vistas.bandeja(
        ctx, usuario, usuario.getEntidadId().map(bandeja::deEntidad).orElse(List.of()), bandeja);
  }

  private List<Donacion> asignadas(Usuario usuario) {
    return usuario.getEntidadId().map(donaciones::getDonacionesDeEntidad).orElse(List.of());
  }

  /** La entidad de la sesión. Una cuenta sin entidad vinculada no puede cargar necesidades. */
  private EntidadBeneficiaria propia(Usuario usuario) {
    return usuario
        .getEntidadId()
        .flatMap(entidades::findById)
        .orElseThrow(() -> new ForbiddenResponse("La cuenta no tiene una entidad vinculada."));
  }

  private Map<String, Object> modeloNecesidades(
      Usuario usuario, EntidadBeneficiaria entidad, Map<String, String> datos) {
    Map<String, Object> modelo = Vistas.modelo(usuario);
    modelo.put("entidad", entidad.getRazonSocial());
    modelo.put(
        "necesidades",
        Stream.concat(
                entidad.getNecesidadesExtraordinarias().stream(),
                entidad.getNecesidadesRecurrentes().stream())
            .map(NecesidadVista::new)
            .toList());
    modelo.put("datos", datos);
    modelo.put(
        "categorias",
        Arrays.stream(Categoria.values())
            .map(
                c ->
                    Vistas.opcion(
                        c.name(),
                        Vistas.nombreCategoria(c),
                        c.name().equals(datos.get("categoria"))))
            .toList());
    modelo.put(
        "unidades",
        Arrays.stream(UnidadMedida.values())
            .map(
                u ->
                    Vistas.opcion(
                        u.name(), u.name().toLowerCase(), u.name().equals(datos.get("unidad"))))
            .toList());
    modelo.put(
        "sugerencias",
        subcategorias.getSubcategorias().stream()
            .map(SubCategoria::getNombre)
            .distinct()
            .sorted()
            .toList());
    modelo.put("recurrente", "RECURRENTE".equals(datos.get("tipo")));
    modelo.put("mensual", "MENSUAL".equals(datos.get("periodo")));
    return modelo;
  }

  private static double cantidad(String valor) {
    try {
      double cantidad = Double.parseDouble(valor.replace(',', '.'));
      if (!Double.isFinite(cantidad) || cantidad <= 0) {
        throw new IllegalArgumentException("La cantidad tiene que ser mayor a cero.");
      }
      return cantidad;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("La cantidad tiene que ser un número.");
    }
  }

  private static String obligatorio(String valor, String mensaje) {
    if (valor == null || valor.isBlank()) {
      throw new IllegalArgumentException(mensaje);
    }
    return valor;
  }

  private static <E extends Enum<E>> E enumerado(Class<E> tipo, String valor, String campo) {
    try {
      return Enum.valueOf(tipo, valor);
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new IllegalArgumentException("Elegí " + campo + ".");
    }
  }

  /** Una necesidad en la lista de la entidad (getters para Handlebars). */
  public static final class NecesidadVista {
    private final Necesidad necesidad;

    NecesidadVista(Necesidad necesidad) {
      this.necesidad = necesidad;
    }

    public String getId() {
      return necesidad.getId().toString();
    }

    public String getQue() {
      return necesidad.getSubcategoria().getNombre();
    }

    public String getCategoria() {
      return Vistas.nombreCategoria(necesidad.getSubcategoria().getCategoria());
    }

    public String getDescripcion() {
      return necesidad.getDescripcion();
    }

    public String getProgreso() {
      return numero(necesidad.getCantidadRecibida())
          + " de "
          + numero(necesidad.getCantidadObjetivo())
          + " "
          + necesidad.getUnidad().name().toLowerCase();
    }

    public long getPorcentaje() {
      return Math.min(
          100, Math.round(100 * necesidad.getCantidadRecibida() / necesidad.getCantidadObjetivo()));
    }

    public boolean isSatisfecha() {
      return necesidad.estaSatisfecha();
    }

    public String getTipo() {
      if (necesidad instanceof NecesidadRecurrente recurrente) {
        return recurrente.getPeriodo() == TipoPeriodo.MENSUAL
            ? "Todos los meses"
            : "Todas las semanas";
      }
      return "Por única vez";
    }

    private static String numero(double valor) {
      return valor == Math.rint(valor) ? String.valueOf((long) valor) : String.valueOf(valor);
    }
  }
}
