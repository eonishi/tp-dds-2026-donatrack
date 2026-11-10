package ar.edu.utn.donatrack.security;

import io.javalin.security.RouteRole;

/**
 * Servicios que entran a la API sin usuario, con la clave interna. No es un rol de persona: no se
 * le asigna a ningún {@link Usuario}, solo se declara en las rutas que el servicio puede usar.
 */
public enum ServicioInterno implements RouteRole {
  LOGISTICA
}
