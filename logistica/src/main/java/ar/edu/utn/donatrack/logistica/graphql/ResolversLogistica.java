package ar.edu.utn.donatrack.logistica.graphql;

import ar.edu.utn.donatrack.logistica.Camion;
import ar.edu.utn.donatrack.logistica.CamionRepository;
import ar.edu.utn.donatrack.logistica.Destino;
import ar.edu.utn.donatrack.logistica.Repartidor;
import ar.edu.utn.donatrack.logistica.RepartidorRepository;
import ar.edu.utn.donatrack.logistica.Ruta;
import ar.edu.utn.donatrack.logistica.RutaRepository;
import ar.edu.utn.donatrack.logistica.monitoreo.Coordenada;
import ar.edu.utn.donatrack.logistica.monitoreo.LecturaPosicion;
import ar.edu.utn.donatrack.logistica.monitoreo.MonitorRutas;
import ar.edu.utn.donatrack.logistica.planificacion.DonacionesRemotas;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Resolvers del esquema {@code logistica.graphqls}. Tienen la misma lógica que tenían los
 * controladores REST de camiones, rutas, repartidores y posiciones, a los que reemplazan: cada
 * mutación es dueña de su transacción.
 */
public class ResolversLogistica implements WithSimplePersistenceUnit {

  private final CamionRepository camiones;
  private final RutaRepository rutas;
  private final RepartidorRepository repartidores;
  private final MonitorRutas monitor;
  private final DonacionesRemotas donacionesRemotas;

  public ResolversLogistica(
      CamionRepository camiones,
      RutaRepository rutas,
      RepartidorRepository repartidores,
      MonitorRutas monitor,
      DonacionesRemotas donacionesRemotas) {
    this.camiones = camiones;
    this.rutas = rutas;
    this.repartidores = repartidores;
    this.monitor = monitor;
    this.donacionesRemotas = donacionesRemotas;
  }

  // ── Consultas ───────────────────────────────────────

  DataFetcher<List<Camion>> camiones() {
    return env -> {
      Acceso.exigirPersona(env);
      return camiones.listarTodos();
    };
  }

  DataFetcher<Camion> camion() {
    return env -> {
      Acceso.exigirPersona(env);
      return camiones.buscarPorPatente(env.getArgument("patente")).orElse(null);
    };
  }

  DataFetcher<List<Ruta>> rutas() {
    return env -> {
      Acceso.exigirPersona(env);
      List<Ruta> todas = rutas.listarTodas();
      // El repartidor ve en su aplicación solo las rutas que tiene asignadas.
      return Acceso.repartidor(env)
          .map(id -> todas.stream().filter(r -> r.tieneAsignado(id)).toList())
          .orElse(todas);
    };
  }

  DataFetcher<Ruta> ruta() {
    return env -> {
      Ruta ruta = rutas.buscarPorId(uuid(env.getArgument("id"))).orElse(null);
      if (ruta != null) {
        Acceso.exigirRutaPropia(env, ruta);
      } else {
        Acceso.exigirPersona(env);
      }
      return ruta;
    };
  }

  /** La consultan la administración, los repartidores y donaciones (con la clave interna). */
  DataFetcher<Map<String, Object>> posicion() {
    return env -> {
      Camion camion = camionExistente(env.getArgument("patente"));
      return monitor
          .ultimaPosicion(camion)
          .map(
              lectura -> {
                Map<String, Object> posicion = new HashMap<>();
                posicion.put("patente", camion.getPatente());
                posicion.put(
                    "rutaActiva", rutas.buscarPorCamion(camion).map(Ruta::getId).orElse(null));
                posicion.put("latitud", lectura.coordenada().latitud());
                posicion.put("longitud", lectura.coordenada().longitud());
                posicion.put("actualizadaEn", lectura.timestamp().toString());
                return posicion;
              })
          .orElse(null);
    };
  }

  DataFetcher<List<Repartidor>> repartidores() {
    return env -> {
      Acceso.exigirAdministrador(env);
      return repartidores.listarTodos();
    };
  }

  // ── Mutaciones de la flota ───────────────────────────

  DataFetcher<Camion> crearCamion() {
    return env -> {
      Acceso.exigirAdministrador(env);
      Camion camion = camionDesde(env.getArgument("input"));
      if (camiones.buscarPorPatente(camion.getPatente()).isPresent()) {
        throw new ErrorLogistica(
            ErrorLogistica.Codigo.DATOS_INVALIDOS,
            "Ya existe un camión con la patente " + camion.getPatente() + ".");
      }
      withTransaction(() -> camiones.agregar(camion));
      return camion;
    };
  }

  DataFetcher<Camion> actualizarCamion() {
    return env -> {
      Acceso.exigirAdministrador(env);
      Camion camion = camionDesde(env.getArgument("input"));
      camionExistente(camion.getPatente());
      return withTransaction(() -> camiones.actualizar(camion));
    };
  }

  DataFetcher<Boolean> eliminarCamion() {
    return env -> {
      Acceso.exigirAdministrador(env);
      String patente = env.getArgument("patente");
      if (camiones.buscarPorPatente(patente).isEmpty()) {
        return false;
      }
      withTransaction(() -> camiones.eliminar(patente));
      return true;
    };
  }

  // ── Mutaciones de rutas ──────────────────────────────

  DataFetcher<Ruta> crearRuta() {
    return env -> {
      Acceso.exigirAdministrador(env);
      Map<String, Object> input = env.getArgument("input");
      Camion camion =
          camiones
              .buscarPorPatente((String) input.get("patenteCamion"))
              .orElseThrow(
                  () ->
                      new ErrorLogistica(
                          ErrorLogistica.Codigo.DATOS_INVALIDOS,
                          "No se puede crear la ruta: Camión inexistente"));
      List<Destino> destinos = destinosDesde(input.get("destinos"));
      return withTransaction(
          () -> {
            Ruta ruta = new Ruta(camion, destinos);
            rutas.agregar(ruta);
            return ruta;
          });
    };
  }

  DataFetcher<Boolean> eliminarRuta() {
    return env -> {
      Acceso.exigirAdministrador(env);
      UUID id = uuid(env.getArgument("id"));
      if (rutas.buscarPorId(id).isEmpty()) {
        return false;
      }
      withTransaction(() -> rutas.eliminar(id));
      return true;
    };
  }

  DataFetcher<Ruta> asignarRepartidor() {
    return env -> {
      Acceso.exigirAdministrador(env);
      Ruta ruta = rutaExistente(env.getArgument("rutaId"));
      Repartidor repartidor =
          repartidores
              .buscarPorId(uuid(env.getArgument("repartidorId")))
              .orElseThrow(
                  () ->
                      new ErrorLogistica(
                          ErrorLogistica.Codigo.DATOS_INVALIDOS, "Repartidor inexistente."));
      withTransaction(() -> ruta.asignarRepartidor(repartidor));
      return ruta;
    };
  }

  DataFetcher<Ruta> iniciarRuta() {
    return env -> {
      Ruta ruta = rutaExistente(env.getArgument("id"));
      Acceso.exigirRutaPropia(env, ruta);
      // El cambio de estado y la lectura de los destinos van en la transacción; el aviso a
      // donaciones sale después de guardarlo.
      List<UUID> donaciones =
          withTransaction(
              () -> {
                ruta.iniciar();
                return ruta.getDestinos().stream().flatMap(d -> d.donaciones().stream()).toList();
              });
      donacionesRemotas.notificarInicioTraslado(donaciones, ruta.getCamion().getPatente());
      return ruta;
    };
  }

  DataFetcher<Ruta> finalizarRuta() {
    return env -> {
      Ruta ruta = rutaExistente(env.getArgument("id"));
      Acceso.exigirRutaPropia(env, ruta);
      withTransaction(ruta::finalizar);
      return ruta;
    };
  }

  // ── Posiciones ───────────────────────────────────────

  DataFetcher<Boolean> reportarPosicion() {
    return env -> {
      Acceso.exigirPersona(env);
      Map<String, Object> input = env.getArgument("input");
      Camion camion = camionExistente((String) input.get("patente"));
      LocalDateTime timestamp;
      try {
        timestamp = LocalDateTime.parse((String) input.get("timestamp"));
      } catch (DateTimeParseException e) {
        throw new ErrorLogistica(
            ErrorLogistica.Codigo.DATOS_INVALIDOS,
            "timestamp debe tener formato ISO-8601, por ejemplo 2026-09-11T11:59:00.");
      }
      Coordenada coordenada =
          new Coordenada(numero(input.get("latitud")), numero(input.get("longitud")));
      // El monitor decide si procesa la lectura o la descarta (futura o sin ruta en curso).
      monitor.procesar(
          new LecturaPosicion(camion, coordenada, numero(input.get("velocidad")), timestamp));
      return true;
    };
  }

  // ── Campos de Destino (sus accesores no siguen la convención getX) ──

  static Object entidadId(DataFetchingEnvironment env) {
    return ((Destino) env.getSource()).entidadId();
  }

  static Object direccion(DataFetchingEnvironment env) {
    return ((Destino) env.getSource()).direccion();
  }

  static Object donaciones(DataFetchingEnvironment env) {
    return ((Destino) env.getSource()).donaciones();
  }

  // ── Auxiliares ───────────────────────────────────────

  private Camion camionExistente(String patente) {
    return camiones
        .buscarPorPatente(patente)
        .orElseThrow(
            () -> new ErrorLogistica(ErrorLogistica.Codigo.NO_ENCONTRADO, "Camión no encontrado"));
  }

  private Ruta rutaExistente(String id) {
    return rutas
        .buscarPorId(uuid(id))
        .orElseThrow(
            () -> new ErrorLogistica(ErrorLogistica.Codigo.NO_ENCONTRADO, "Ruta no encontrada"));
  }

  private static Camion camionDesde(Map<String, Object> input) {
    return new Camion(
        (String) input.get("patente"),
        numero(input.get("volumenM3")),
        numero(input.get("alturaM")),
        numero(input.get("capacidadCargaKg")));
  }

  @SuppressWarnings("unchecked")
  private static List<Destino> destinosDesde(Object destinos) {
    return ((List<Map<String, Object>>) destinos)
        .stream()
            .map(
                d ->
                    new Destino(
                        Long.parseLong((String) d.get("entidadId")),
                        (String) d.get("direccion"),
                        ((List<String>) d.get("donaciones"))
                            .stream().map(ResolversLogistica::uuid).toList()))
            .toList();
  }

  private static UUID uuid(String valor) {
    try {
      return UUID.fromString(valor);
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new ErrorLogistica(ErrorLogistica.Codigo.DATOS_INVALIDOS, "Formato de UUID inválido");
    }
  }

  // GraphQL entrega los Float como Double, pero un literal entero puede llegar como Integer.
  private static double numero(Object valor) {
    return ((Number) valor).doubleValue();
  }
}
