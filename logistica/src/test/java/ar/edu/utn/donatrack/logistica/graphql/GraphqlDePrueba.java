package ar.edu.utn.donatrack.logistica.graphql;

import ar.edu.utn.donatrack.logistica.CamionRepository;
import ar.edu.utn.donatrack.logistica.Repartidor;
import ar.edu.utn.donatrack.logistica.RepartidorRepository;
import ar.edu.utn.donatrack.logistica.RutaRepository;
import ar.edu.utn.donatrack.logistica.controllers.RepartidorController;
import ar.edu.utn.donatrack.logistica.monitoreo.MonitorRutas;
import ar.edu.utn.donatrack.logistica.planificacion.DonacionesRemotas;
import ar.edu.utn.donatrack.logistica.security.LogisticaSecurity;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.testtools.HttpClient;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Arma la app de logística con GraphQL para las pruebas y hace los pedidos. */
public final class GraphqlDePrueba {
  public static final String CLAVE_INTERNA = "clave-interna-de-prueba";
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private GraphqlDePrueba() {}

  public static Javalin app(
      CamionRepository camiones,
      RutaRepository rutas,
      RepartidorRepository repartidores,
      MonitorRutas monitor,
      DonacionesRemotas donacionesRemotas) {
    GraphqlController graphql =
        new GraphqlController(
            GraphqlLogistica.crear(
                new ResolversLogistica(camiones, rutas, repartidores, monitor, donacionesRemotas)));
    RepartidorController sesion = new RepartidorController(repartidores, Clock.systemUTC());
    return Javalin.create()
        .before(LogisticaSecurity::autorizar)
        .post("/logistica/graphql", graphql::ejecutar)
        .post("/logistica/login", sesion::login)
        .post("/logistica/logout", sesion::logout);
  }

  /** Token de una persona administradora, como el que emite donaciones. */
  public static String admin() {
    return "Bearer "
        + JWT.create()
            .withSubject("admin")
            .withClaim("roles", List.of("ADMIN"))
            .withExpiresAt(Instant.now().plusSeconds(600))
            .sign(Algorithm.HMAC256("donatrack-dev-secret-change-me"));
  }

  public static String repartidor(Repartidor repartidor) {
    return "Bearer " + LogisticaSecurity.emitirToken(repartidor, Instant.now());
  }

  /** Respuesta de un pedido GraphQL: código HTTP y cuerpo. */
  public record Respuesta(int status, JsonNode cuerpo) {
    public JsonNode data() {
      return cuerpo.path("data");
    }

    /** Código del primer error ({@code extensions.codigo}), o null si no hubo errores. */
    public String codigoDeError() {
      JsonNode errores = cuerpo.path("errors");
      return errores.isArray() && errores.size() > 0
          ? errores.get(0).path("extensions").path("codigo").asText(null)
          : null;
    }

    public String mensajeDeError() {
      return cuerpo.path("errors").path(0).path("message").asText(null);
    }
  }

  public static Respuesta pedir(
      HttpClient client, String autorizacion, String query, Map<String, Object> variables)
      throws Exception {
    Map<String, Object> pedido = new HashMap<>();
    pedido.put("query", query);
    pedido.put("variables", variables);
    try (var response =
        client.post(
            "/logistica/graphql",
            MAPPER.writeValueAsString(pedido),
            req -> {
              if (autorizacion != null) {
                req.header("Authorization", autorizacion);
              }
            })) {
      String cuerpo = response.body() == null ? "" : response.body().string();
      return new Respuesta(response.code(), json(cuerpo));
    }
  }

  // Los rechazos del filtro (401/403) vienen como texto plano, no como JSON de GraphQL.
  private static JsonNode json(String cuerpo) {
    try {
      return cuerpo.isBlank() ? MAPPER.createObjectNode() : MAPPER.readTree(cuerpo);
    } catch (Exception e) {
      return MAPPER.createObjectNode().put("texto", cuerpo);
    }
  }

  public static Respuesta pedir(HttpClient client, String autorizacion, String query)
      throws Exception {
    return pedir(client, autorizacion, query, Map.of());
  }
}
