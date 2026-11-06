package ar.edu.utn.donatrack.logistica.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ar.edu.utn.donatrack.logistica.MainLogistica;
import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LogisticaSecurityHttpTest {
  private final HttpClient client = HttpClient.newHttpClient();
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
        HttpRequest.newBuilder(URI.create(baseUrl + "/logistica/camiones"))
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
        HttpRequest.newBuilder(URI.create(baseUrl + "/logistica/camiones")).GET().build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(401, response.statusCode());
  }
}
