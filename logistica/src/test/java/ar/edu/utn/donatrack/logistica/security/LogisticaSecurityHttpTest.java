package ar.edu.utn.donatrack.logistica.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import ar.edu.utn.donatrack.logistica.MainLogistica;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LogisticaSecurityHttpTest {
  private static final String SECRET =
      System.getenv().getOrDefault("DONATRACK_JWT_SECRET", "donatrack-dev-secret-change-me");
  private final HttpClient client = HttpClient.newHttpClient();
  // Las operaciones de logística van por GraphQL: se prueba con la consulta de la flota.
  private static final String CONSULTA_CAMIONES = "{\"query\":\"{ camiones { patente } }\"}";

  private Javalin app;
  private String baseUrl;

  @BeforeEach
  void start() {
    app = new MainLogistica().crearApp().start(0);
    baseUrl = "http://localhost:" + app.port();
  }

  @AfterEach
  void stop() {
    app.stop();
  }

  @Test
  void preflightCorsOptionsResponde204ConHeadersCorsSinRequerirSesion() throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(baseUrl + "/logistica/graphql"))
            .header("Origin", "http://localhost:7070")
            .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
            .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(204, response.statusCode());
    assertEquals(
        "http://localhost:7070",
        response.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
    assertEquals(
        "true", response.headers().firstValue("Access-Control-Allow-Credentials").orElse(""));
  }

  @Test
  void rutaDeLogisticaSinCredencialesResponde401() throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(baseUrl + "/logistica/graphql"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(CONSULTA_CAMIONES))
            .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(401, response.statusCode());
  }

  @Test
  void tokenFirmadoConRolAdminPuedeAccederALogistica() throws Exception {
    String token =
        JWT.create()
            .withClaim("roles", List.of("ADMIN"))
            .withExpiresAt(Instant.now().plusSeconds(60))
            .sign(Algorithm.HMAC256(SECRET));
    int status = getConCookie(token).statusCode();
    assertNotEquals(401, status);
    assertNotEquals(403, status);
  }

  @Test
  void tokenConFirmaInvalidaSeRechazaCon401() throws Exception {
    String token =
        JWT.create()
            .withClaim("roles", List.of("ADMIN"))
            .withExpiresAt(Instant.now().plusSeconds(60))
            .sign(Algorithm.HMAC256("otro-secreto"));
    assertEquals(401, getConCookie(token).statusCode());
  }

  @Test
  void tokenSinExpiracionSeRechazaCon401() throws Exception {
    String token =
        JWT.create().withClaim("roles", List.of("ADMIN")).sign(Algorithm.HMAC256(SECRET));
    assertEquals(401, getConCookie(token).statusCode());
  }

  @Test
  void tokenConRolesMalFormadosSeRechazaCon403() throws Exception {
    String token =
        JWT.create()
            .withClaim("roles", List.of(Map.of("rol", "ADMIN")))
            .withExpiresAt(Instant.now().plusSeconds(60))
            .sign(Algorithm.HMAC256(SECRET));
    assertEquals(403, getConCookie(token).statusCode());
  }

  private HttpResponse<String> getConCookie(String token) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(baseUrl + "/logistica/graphql"))
            .header("Cookie", "access_token=" + token)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(CONSULTA_CAMIONES))
            .build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
