package ar.edu.utn.donatrack.web.controller;

import static io.javalin.apibuilder.ApiBuilder.get;

import ar.edu.utn.donatrack.beneficiarios.BeneficiariosRepository;
import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donantes.MedioDeContacto;
import ar.edu.utn.donatrack.security.AuthService;
import ar.edu.utn.donatrack.security.Role;
import io.javalin.http.Context;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * La administración consulta las entidades beneficiarias cargadas en el sistema: listado paginado
 * con búsqueda, solo lectura. Es la contracara de {@link AdminDonantesController}: la
 * administración ve a donantes y entidades desde acá, no entrando a las pantallas de cada una.
 */
public final class AdminEntidadesController {
  static final int POR_PAGINA = 25;

  private final AuthService authService;
  private final BeneficiariosRepository entidades;

  public AdminEntidadesController(AuthService authService, BeneficiariosRepository entidades) {
    this.authService = authService;
    this.entidades = entidades;
  }

  public void routes() {
    get("/admin/entidades", this::listar, Role.ADMIN);
  }

  private void listar(Context ctx) {
    Map<String, Object> modelo = Vistas.modelo(Vistas.usuario(ctx));
    String busqueda = ctx.queryParam("q") == null ? "" : ctx.queryParam("q").trim();
    int desde =
        Vistas.paginar(
            modelo,
            "/admin/entidades",
            busqueda,
            entidades.contar(busqueda),
            ctx.queryParam("pagina"),
            POR_PAGINA);

    List<EntidadBeneficiaria> encontradas = entidades.buscar(busqueda, desde, POR_PAGINA);
    Set<Long> conCuenta =
        authService.entidadesConCuenta(
            encontradas.stream().map(EntidadBeneficiaria::getId).toList());
    modelo.put(
        "entidades",
        encontradas.stream().map(e -> fila(e, conCuenta.contains(e.getId()))).toList());
    ctx.render("admin-entidades", modelo);
  }

  /** Lo que se muestra de cada entidad en la tabla. */
  public record Fila(
      String razonSocial,
      String direccion,
      String correos,
      String telefono,
      String ubicacion,
      String ubicacionClase,
      String cuenta,
      String cuentaClase) {}

  private static Fila fila(EntidadBeneficiaria entidad, boolean tieneCuenta) {
    boolean ubicada = entidad.getLatitud().isPresent() && entidad.getLongitud().isPresent();
    return new Fila(
        entidad.getRazonSocial(),
        entidad.getDireccion(),
        entidad.getCorreos().stream()
            .map(MedioDeContacto::getContacto)
            .collect(Collectors.joining(", ")),
        entidad.getTelefono(),
        ubicada ? "En el mapa" : "Sin ubicar",
        ubicada ? "text-bg-success" : "text-bg-secondary",
        tieneCuenta ? "Con cuenta" : "Sin cuenta",
        tieneCuenta ? "text-bg-success" : "text-bg-secondary");
  }
}
