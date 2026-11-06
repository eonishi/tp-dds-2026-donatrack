package ar.edu.utn.donatrack.web.controller;

import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.EstadoDonacion;
import ar.edu.utn.donatrack.donaciones.HistorialEstado;
import ar.edu.utn.donatrack.security.AuthService;
import ar.edu.utn.donatrack.security.Role;
import ar.edu.utn.donatrack.security.Usuario;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;
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
   * La persona con sesión, si tiene el rol pedido (los administradores pasan siempre). El filtro de
   * rutas protegidas ya redirige al login a quien no tiene sesión.
   */
  static Usuario exigir(Context ctx, AuthService authService, Role rol) {
    Usuario usuario =
        authService.currentUser(ctx).orElseThrow(() -> new UnauthorizedResponse("Sin sesión."));
    if (!usuario.hasRole(rol) && !usuario.hasRole(Role.ADMIN)) {
      throw new ForbiddenResponse("Sin permiso.");
    }
    return usuario;
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
}
