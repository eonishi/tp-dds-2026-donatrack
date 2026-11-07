package ar.edu.utn.donatrack.logistica.graphql;

import ar.edu.utn.donatrack.logistica.Ruta;
import ar.edu.utn.donatrack.logistica.security.LogisticaSecurity;
import graphql.schema.DataFetchingEnvironment;
import io.javalin.http.Context;
import java.util.Optional;
import java.util.UUID;

/**
 * Permisos por operación. GraphQL expone un solo endpoint, así que el filtro HTTP solo verifica que
 * haya credenciales válidas, y cada resolver decide quién puede usarlo.
 */
final class Acceso {
  private Acceso() {}

  static Context ctx(DataFetchingEnvironment env) {
    return env.getGraphQlContext().get(Context.class);
  }

  static boolean esAdministrador(DataFetchingEnvironment env) {
    return LogisticaSecurity.esAdministrador(ctx(env));
  }

  /** El repartidor autenticado, o vacío si quien llama es ADMIN o donaciones. */
  static Optional<UUID> repartidor(DataFetchingEnvironment env) {
    return Optional.ofNullable(ctx(env).attribute(LogisticaSecurity.ATRIBUTO_REPARTIDOR));
  }

  /** Operaciones de gestión: administrar la flota y las rutas, y asignar repartidores. */
  static void exigirAdministrador(DataFetchingEnvironment env) {
    if (!esAdministrador(env)) {
      throw new ErrorLogistica(
          ErrorLogistica.Codigo.PROHIBIDO, "Solo la administración puede hacer esta operación.");
    }
  }

  /** Operaciones de personas (ADMIN o REPARTIDOR): no las usa donaciones con la clave interna. */
  static void exigirPersona(DataFetchingEnvironment env) {
    if (!esAdministrador(env) && repartidor(env).isEmpty()) {
      throw new ErrorLogistica(
          ErrorLogistica.Codigo.PROHIBIDO, "No tiene permisos para esta operación.");
    }
  }

  /** El repartidor solo opera las rutas que tiene asignadas; ADMIN, todas. */
  static void exigirRutaPropia(DataFetchingEnvironment env, Ruta ruta) {
    exigirPersona(env);
    Optional<UUID> repartidor = repartidor(env);
    if (repartidor.isPresent() && !ruta.tieneAsignado(repartidor.get())) {
      throw new ErrorLogistica(
          ErrorLogistica.Codigo.PROHIBIDO, "El repartidor no está asignado a esta ruta.");
    }
  }
}
