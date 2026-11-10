package ar.edu.utn.donatrack.api.controller;

import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.security.ControlDeAcceso;
import ar.edu.utn.donatrack.security.Role;
import ar.edu.utn.donatrack.security.Usuario;
import io.javalin.http.Context;
import java.util.Optional;

/**
 * Qué donaciones puede ver u operar quien hace el request.
 *
 * <p>La persona administradora y logística (que llama con la clave interna y no trae usuario) ven
 * todas. La persona donante ve las que donó y la entidad beneficiaria las que le asignaron.
 */
final class AccesoDonaciones {
  private AccesoDonaciones() {}

  static Optional<Usuario> usuario(Context ctx) {
    return ControlDeAcceso.usuario(ctx);
  }

  static boolean accesoCompleto(Context ctx) {
    return usuario(ctx).map(u -> u.hasRole(Role.ADMIN)).orElse(true);
  }

  static boolean puedeVer(Context ctx, Donacion donacion) {
    return accesoCompleto(ctx)
        || usuario(ctx)
            .map(u -> esDonante(u, donacion) || esBeneficiaria(u, donacion))
            .orElse(false);
  }

  /** La entidad beneficiaria opera sobre las donaciones que le asignaron. */
  static boolean puedeOperarComoBeneficiaria(Context ctx, Donacion donacion) {
    return accesoCompleto(ctx) || usuario(ctx).map(u -> esBeneficiaria(u, donacion)).orElse(false);
  }

  private static boolean esDonante(Usuario usuario, Donacion donacion) {
    return usuario.hasRole(Role.DONANTE)
        && donacion.getDonante() != null
        && usuario.getDonanteId().map(donacion.getDonante().getId()::equals).orElse(false);
  }

  private static boolean esBeneficiaria(Usuario usuario, Donacion donacion) {
    return usuario.hasRole(Role.BENEFICIARIO)
        && donacion.getEntidadBeneficiaria() != null
        && usuario
            .getEntidadId()
            .map(donacion.getEntidadBeneficiaria().getId()::equals)
            .orElse(false);
  }
}
