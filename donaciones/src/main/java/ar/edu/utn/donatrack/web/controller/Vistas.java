package ar.edu.utn.donatrack.web.controller;

import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.bienes.Categoria;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.EstadoDonacion;
import ar.edu.utn.donatrack.donaciones.HistorialEstado;
import ar.edu.utn.donatrack.notificaciones.AvisoBandeja;
import ar.edu.utn.donatrack.notificaciones.BandejaNotificacionesRepository;
import ar.edu.utn.donatrack.security.ControlDeAcceso;
import ar.edu.utn.donatrack.security.Usuario;
import io.javalin.http.Context;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Ayudas comunes de los controllers de vistas: sesión, modelo base y formatos para mostrar. */
final class Vistas {
  static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private Vistas() {}

  /**
   * La persona con sesión. Los permisos ya los verificó {@link ControlDeAcceso} con los roles que
   * declara la ruta, así que acá solo se la toma del request.
   */
  static Usuario usuario(Context ctx) {
    return ControlDeAcceso.usuarioObligatorio(ctx);
  }

  /**
   * Dirección absoluta de {@code ruta} para los enlaces que salen por correo o mensaje. Usa
   * DONATRACK_WEB_ORIGIN si está definida (detrás de un proxy el host del pedido no sirve).
   */
  static String urlPublica(Context ctx, String ruta) {
    String origen = System.getenv("DONATRACK_WEB_ORIGIN");
    String base = origen == null || origen.isBlank() ? ctx.scheme() + "://" + ctx.host() : origen;
    return base.replaceAll("/+$", "") + ruta;
  }

  /**
   * Pagina un listado con búsqueda: deja en el modelo lo que muestra el partial {@code paginacion}
   * (búsqueda, total, página actual y enlaces a la anterior y la siguiente) y devuelve desde qué
   * fila traer. Una página pedida inválida o fuera de rango se corrige a la más cercana.
   */
  static int paginar(
      Map<String, Object> modelo,
      String ruta,
      String busqueda,
      long total,
      String paginaPedida,
      int porPagina) {
    int paginas = (int) Math.max(1, (total + porPagina - 1) / porPagina);
    int pagina = Math.min(paginas, Math.max(1, numero(paginaPedida)));
    modelo.put("q", busqueda);
    modelo.put("total", total);
    modelo.put("pagina", pagina);
    modelo.put("paginas", paginas);
    String enlace = ruta + "?q=" + URLEncoder.encode(busqueda, StandardCharsets.UTF_8) + "&pagina=";
    if (pagina > 1) {
      modelo.put("anterior", enlace + (pagina - 1));
    }
    if (pagina < paginas) {
      modelo.put("siguiente", enlace + (pagina + 1));
    }
    return (pagina - 1) * porPagina;
  }

  private static int numero(String valor) {
    try {
      return valor == null ? 1 : Integer.parseInt(valor.trim());
    } catch (NumberFormatException e) {
      return 1;
    }
  }

  static Map<String, Object> modelo(Usuario usuario) {
    Map<String, Object> modelo = new HashMap<>();
    modelo.put("usuario", usuario);
    return modelo;
  }

  /** "3 kilogramos de Arroz". */
  static String describir(Bien bien) {
    double cantidad = bien.getCantidad();
    String numero =
        cantidad == Math.rint(cantidad)
            ? String.valueOf((long) cantidad)
            : String.valueOf(cantidad);
    return numero
        + " "
        + bien.getUnidad().name().toLowerCase().replace('_', ' ')
        + " de "
        + bien.getSubcategoria().getNombre();
  }

  static List<String> bienes(Donacion donacion) {
    return donacion.getBienes().stream().map(Vistas::describir).toList();
  }

  /** Cuándo llegó la donación a {@code estado} por última vez. */
  static Optional<LocalDateTime> fechaDe(Donacion donacion, EstadoDonacion estado) {
    return donacion.getHistorialEstado().stream()
        .filter(h -> h.getEstadoAlcanzado() == estado)
        .map(HistorialEstado::getFecha)
        .max(Comparator.naturalOrder());
  }

  /** Último cambio de estado, para ordenar de lo más reciente a lo más viejo. */
  static LocalDateTime ultimoCambio(Donacion donacion) {
    return donacion.getHistorialEstado().stream()
        .map(HistorialEstado::getFecha)
        .max(Comparator.naturalOrder())
        .orElse(LocalDateTime.MIN);
  }

  /** Nombre legible de un estado, para chips y filtros. */
  static String nombre(EstadoDonacion estado) {
    return switch (estado) {
      case EN_DEPOSITO -> "En depósito";
      case ASIGNACION_REALIZADA -> "Asignada";
      case LISTA_PARA_ENTREGAR -> "Lista para entregar";
      case EN_TRASLADO -> "En camino";
      case ENTREGADA -> "Entregada";
      case ENTREGA_FALLIDA -> "Entrega con problemas";
      case VENCIDA -> "Vencida";
    };
  }

  /**
   * Solo URLs que la vista puede mostrar en un {@code <img>} sin riesgo: http(s) o las fotos
   * locales que sirve el propio sistema.
   */
  static boolean esFotoMostrable(String url) {
    return url != null && (url.matches("(?i)^https?://.+") || url.matches("^/fotos/[a-z0-9.-]+$"));
  }

  static String nombreCategoria(Categoria categoria) {
    return switch (categoria) {
      case ALIMENTO -> "Alimentos";
      case VESTIMENTA -> "Vestimenta";
      case MOBILIARIO -> "Mobiliario";
    };
  }

  /** Color del chip de cada estado (clases de Bootstrap). */
  static String clase(EstadoDonacion estado) {
    return switch (estado) {
      case ENTREGADA -> "text-bg-success";
      case EN_TRASLADO -> "text-bg-primary";
      case ENTREGA_FALLIDA, VENCIDA -> "text-bg-danger";
      case ASIGNACION_REALIZADA, LISTA_PARA_ENTREGAR -> "text-bg-info";
      case EN_DEPOSITO -> "text-bg-secondary";
    };
  }

  static Opcion opcion(String valor, String texto, boolean elegida) {
    return new Opcion(valor, texto, elegida);
  }

  /** Una opción de un select (getters para Handlebars). */
  public static final class Opcion {
    private final String valor;
    private final String texto;
    private final boolean elegida;

    Opcion(String valor, String texto, boolean elegida) {
      this.valor = valor;
      this.texto = texto;
      this.elegida = elegida;
    }

    public String getValor() {
      return valor;
    }

    public String getTexto() {
      return texto;
    }

    public boolean isElegida() {
      return elegida;
    }
  }

  /**
   * Muestra la bandeja de avisos y los marca como leídos. Los que no se habían leído se resaltan
   * esta vez.
   */
  static void bandeja(
      Context ctx,
      Usuario usuario,
      List<AvisoBandeja> avisos,
      BandejaNotificacionesRepository repositorio) {
    List<AvisoVista> vistas = avisos.stream().map(AvisoVista::new).toList();
    repositorio.marcarLeidos(avisos);
    Map<String, Object> modelo = modelo(usuario);
    modelo.put("avisos", vistas);
    modelo.put("nuevos", vistas.stream().filter(AvisoVista::isNuevo).count());
    ctx.render("notificaciones", modelo);
  }

  /** Un aviso de la bandeja (getters para Handlebars). */
  public static final class AvisoVista {
    private final String mensaje;
    private final String fecha;
    private final boolean nuevo;

    AvisoVista(AvisoBandeja aviso) {
      this.mensaje = aviso.getMensaje();
      this.fecha = FECHA_HORA.format(aviso.getFecha());
      this.nuevo = !aviso.isLeido();
    }

    public String getMensaje() {
      return mensaje;
    }

    public String getFecha() {
      return fecha;
    }

    public boolean isNuevo() {
      return nuevo;
    }
  }
}
