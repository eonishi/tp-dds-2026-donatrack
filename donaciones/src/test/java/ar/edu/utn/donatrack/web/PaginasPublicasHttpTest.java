package ar.edu.utn.donatrack.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

class PaginasPublicasHttpTest {
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
  void laLandingSeRenderizaConElLayoutYEnlazaLasSeccionesPublicas() throws Exception {
    HttpResponse<String> response = get("/");

    assertEquals(200, response.statusCode());
    assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("text/html"));
    String html = response.body();
    assertTrue(html.contains("<nav"), "debería incluir el layout");
    assertTrue(html.contains("href=\"/entregas\""));
    assertTrue(html.contains("href=\"/login\""));
    assertTrue(html.contains("href=\"/registro\""));
    assertTrue(html.contains("href=\"/registro-entidad\""));
    assertTrue(html.contains("href=\"/legal\""));
  }

  @Test
  void lasSeccionesEnlazadasResponden() throws Exception {
    for (String ruta : new String[] {"/legal", "/entregas", "/login", "/registro"}) {
      assertEquals(200, get(ruta).statusCode(), ruta);
    }
  }

  @Test
  void elRegistroUnificaDonantesYEntidadesConUnSelectorDeRol() throws Exception {
    for (String ruta : new String[] {"/registro", "/register", "/registro-entidad"}) {
      HttpResponse<String> response = get(ruta);
      assertEquals(200, response.statusCode(), ruta);

      String html = response.body();
      assertTrue(html.contains("<nav"), ruta + " debería incluir el layout");
      assertTrue(html.contains("name=\"rol\""), ruta + " debería enviar el rol elegido");
      assertTrue(html.contains("id=\"rol-donante\""), ruta + " debería ofrecer el rol donante");
      assertTrue(
          html.contains("id=\"rol-entidad\""),
          ruta + " debería ofrecer el rol entidad beneficiaria");
      assertTrue(
          html.contains("data-rol=\"donante\""), ruta + " debería traer los datos del donante");
      assertTrue(
          html.contains("data-rol=\"entidad\""), ruta + " debería traer los datos de la entidad");
      assertTrue(html.contains("href=\"/login\""), ruta + " debería ofrecer iniciar sesión");
      assertFalse(
          html.contains("Estamos construyendo"), ruta + " ya no debería ser una página próxima");
    }
  }

  @Test
  void losEstaticosSeSirvenDesdeElClasspath() throws Exception {
    assertEquals(200, get("/estilos.css").statusCode());
    assertEquals(200, get("/DonaTrack.png").statusCode());
  }

  private HttpResponse<String> get(String ruta) throws IOException, InterruptedException {
    return client.send(
        HttpRequest.newBuilder(URI.create(baseUrl + ruta)).GET().build(),
        HttpResponse.BodyHandlers.ofString());
  }
}
