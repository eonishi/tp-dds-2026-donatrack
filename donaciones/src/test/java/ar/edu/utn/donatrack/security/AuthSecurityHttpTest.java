package ar.edu.utn.donatrack.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.donatrack.api.DonacionesApi;
import io.javalin.Javalin;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthSecurityHttpTest {
  private final HttpClient client = HttpClient.newHttpClient();
  private Javalin app;
  private String baseUrl;

  @BeforeEach
  void start() {
    app = DonacionesApi.create().start(0);
    baseUrl = "http://localhost:" + app.port();
  }

  @AfterEach
  void stop() {
    app.stop();
  }

  @Test
  void loginConCredencialesValidasGeneraCookieYPermiteAccederAlPanel() throws Exception {
    HttpResponse<String> loginPage = get("/login");
    assertEquals(200, loginPage.statusCode());
    assertTrue(loginPage.body().contains("name=\"email\""));

    HttpResponse<String> loginResponse =
        post(
            "/login",
            "email=admin%40donatrack.local&password=admin123",
            "application/x-www-form-urlencoded");

    assertEquals(302, loginResponse.statusCode());
    assertTrue(
        loginResponse.headers().firstValue("Set-Cookie").orElse("").contains("access_token="));

    HttpResponse<String> dashboard =
        get("/dashboard", loginResponse.headers().firstValue("Set-Cookie").orElse(""));
    assertEquals(200, dashboard.statusCode());
    assertTrue(dashboard.body().contains("Bienvenido"));
  }

  @Test
  void unUsuarioSinPermisoRecibe403EnApiProtegida() throws Exception {
    HttpResponse<String> loginResponse =
        post(
            "/login",
            "email=donante%40donatrack.local&password=donante123",
            "application/x-www-form-urlencoded");

    String cookie = loginResponse.headers().firstValue("Set-Cookie").orElse("");
    HttpResponse<String> response = get("/api/v1/donantes", cookie);
    assertEquals(403, response.statusCode());
  }

  private HttpResponse<String> get(String ruta) throws IOException, InterruptedException {
    return get(ruta, "");
  }

  private HttpResponse<String> get(String ruta, String cookie)
      throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(baseUrl + ruta)).header("Cookie", cookie).GET().build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> post(String ruta, String body, String contentType)
      throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(baseUrl + ruta))
            .header("Content-Type", contentType)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
