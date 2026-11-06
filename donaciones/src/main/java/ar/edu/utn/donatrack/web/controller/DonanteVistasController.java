package ar.edu.utn.donatrack.web.controller;

import static io.javalin.apibuilder.ApiBuilder.get;

import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.bienes.Categoria;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.bienes.SubcategoriasRepository;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.DonacionesRepository;
import ar.edu.utn.donatrack.donaciones.EstadoDonacion;
import ar.edu.utn.donatrack.notificaciones.BandejaNotificacionesRepository;
import ar.edu.utn.donatrack.security.AuthService;
import ar.edu.utn.donatrack.security.Role;
import ar.edu.utn.donatrack.security.Usuario;
import io.javalin.http.Context;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Vistas de la persona donante: sus donaciones, su seguimiento y sus avisos. */
public class DonanteVistasController {
  private final AuthService authService;
  private final DonacionesRepository donaciones;
  private final SubcategoriasRepository subcategorias;
  private final BandejaNotificacionesRepository bandeja;
  private final Seguimiento seguimiento;

  DonanteVistasController(
      AuthService authService,
      DonacionesRepository donaciones,
      SubcategoriasRepository subcategorias,
      BandejaNotificacionesRepository bandeja,
      Seguimiento seguimiento) {
    this.authService = authService;
    this.donaciones = donaciones;
    this.subcategorias = subcategorias;
    this.bandeja = bandeja;
    this.seguimiento = seguimiento;
  }

  public void routes() {
    get("/donante/donaciones", this::misDonaciones);
    get("/donante/seguimiento", this::seguimiento);
    get("/donante/seguimiento/posiciones", this::posiciones);
    get("/donante/notificaciones", this::notificaciones);
  }

  /** Mis donaciones, filtradas por estado y por categoría o subcategorías de lo que se donó. */
  private void misDonaciones(Context ctx) {
    Usuario usuario = Vistas.exigir(ctx, authService, Role.DONANTE);
    Optional<EstadoDonacion> estado = enumerado(EstadoDonacion.class, ctx.queryParam("estado"));
    Optional<Categoria> categoria = enumerado(Categoria.class, ctx.queryParam("categoria"));
    Set<String> elegidas =
        ctx.queryParams("subcategoria").stream()
            .filter(s -> !s.isBlank())
            .collect(Collectors.toSet());

    List<Donacion> todas = propias(usuario);
    List<DonacionVista> filtradas =
        todas.stream()
            .filter(d -> estado.map(e -> d.getEstado() == e).orElse(true))
            .filter(d -> categoria.map(c -> tieneBien(d, b -> c == categoriaDe(b))).orElse(true))
            .filter(
                d ->
                    elegidas.isEmpty()
                        || tieneBien(d, b -> elegidas.contains(b.getSubcategoria().getNombre())))
            .sorted(Comparator.comparing(Vistas::ultimoCambio).reversed())
            .map(DonacionVista::new)
            .toList();

    Map<String, Object> modelo = Vistas.modelo(usuario);
    modelo.put("donaciones", filtradas);
    modelo.put("total", todas.size());
    modelo.put("cantidad", filtradas.size());
    modelo.put("hayFiltros", estado.isPresent() || categoria.isPresent() || !elegidas.isEmpty());
    modelo.put(
        "estados",
        Arrays.stream(EstadoDonacion.values())
            .map(e -> Vistas.opcion(e.name(), Vistas.nombre(e), estado.orElse(null) == e))
            .toList());
    modelo.put(
        "categorias",
        Arrays.stream(Categoria.values())
            .map(
                c ->
                    Vistas.opcion(c.name(), Vistas.nombreCategoria(c), categoria.orElse(null) == c))
            .toList());
    modelo.put(
        "subcategorias",
        subcategorias.getSubcategorias().stream()
            .filter(s -> categoria.map(c -> s.getCategoria() == c).orElse(true))
            .map(SubCategoria::getNombre)
            .distinct()
            .sorted()
            .map(n -> Vistas.opcion(n, n, elegidas.contains(n)))
            .toList());
    ctx.render("donante-donaciones", modelo);
  }

  private void seguimiento(Context ctx) {
    Usuario usuario = Vistas.exigir(ctx, authService, Role.DONANTE);
    Map<String, Object> modelo = Vistas.modelo(usuario);
    modelo.put("entregas", seguimiento.entregas(propias(usuario)));
    modelo.put("posiciones", "/donante/seguimiento/posiciones");
    modelo.put("vistaEntidad", false);
    ctx.render("seguimiento", modelo);
  }

  private void posiciones(Context ctx) {
    Usuario usuario = Vistas.exigir(ctx, authService, Role.DONANTE);
    ctx.json(seguimiento.entregas(propias(usuario)));
  }

  private void notificaciones(Context ctx) {
    Usuario usuario = Vistas.exigir(ctx, authService, Role.DONANTE);
    Vistas.bandeja(
        ctx, usuario, usuario.getDonanteId().map(bandeja::deDonante).orElse(List.of()), bandeja);
  }

  private List<Donacion> propias(Usuario usuario) {
    return usuario.getDonanteId().map(donaciones::getDonacionesDeDonante).orElse(List.of());
  }

  private static boolean tieneBien(Donacion donacion, Predicate<Bien> filtro) {
    return donacion.getBienes().stream().anyMatch(filtro);
  }

  private static Categoria categoriaDe(Bien bien) {
    return bien.getSubcategoria().getCategoria();
  }

  private static <E extends Enum<E>> Optional<E> enumerado(Class<E> tipo, String valor) {
    if (valor == null || valor.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(Enum.valueOf(tipo, valor));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  /** Una donación en la lista de la persona donante (getters para Handlebars). */
  public static final class DonacionVista {
    private final Donacion donacion;

    DonacionVista(Donacion donacion) {
      this.donacion = donacion;
    }

    public String getEstado() {
      return Vistas.nombre(donacion.getEstado());
    }

    public String getClaseEstado() {
      return Vistas.clase(donacion.getEstado());
    }

    public List<String> getBienes() {
      return Vistas.bienes(donacion);
    }

    public String getEntidad() {
      return donacion.getEntidadBeneficiaria() == null
          ? ""
          : donacion.getEntidadBeneficiaria().getRazonSocial();
    }

    public String getActualizada() {
      return Vistas.ultimoCambio(donacion).equals(LocalDateTime.MIN)
          ? ""
          : Vistas.FECHA_HORA.format(Vistas.ultimoCambio(donacion));
    }

    public boolean isEnCamino() {
      return donacion.getEstado() == EstadoDonacion.EN_TRASLADO;
    }

    public List<String> getFotos() {
      return donacion.getFotos().stream().filter(Vistas::esFotoMostrable).toList();
    }

    public String getId() {
      return donacion.getId().toString();
    }
  }
}
