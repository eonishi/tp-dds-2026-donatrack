package ar.edu.utn.donatrack.security;

import io.javalin.security.RouteRole;

/** Rol de una persona usuaria. Las rutas los declaran para decir quién puede usarlas. */
public enum Role implements RouteRole {
  ADMIN,
  DONANTE,
  BENEFICIARIO;
}
