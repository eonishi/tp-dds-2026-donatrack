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
    assertTrue(html.contains("href=\"/legal\""));
  }

  @Test
  void lasSeccionesEnlazadasResponden() throws Exception {
    for (String ruta : new String[] {"/legal", "/entregas", "/login"}) {
      assertEquals(200, get(ruta).statusCode(), ruta);
    }
  }

  @Test
  void laVistaLegalMuestraPoliticaDePrivacidadCookiesIndiceYEnlacesDeVuelta() throws Exception {
    HttpResponse<String> response = get("/legal");
    assertEquals(200, response.statusCode());
    assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("text/html"));

    String html = response.body();
    assertTrue(html.contains("<nav"), "/legal debería incluir el layout");
    assertFalse(
        html.contains("Estamos construyendo"), "/legal ya no debería ser una página próxima");

    assertEquals(1, html.split("<h1[\\s>]", -1).length - 1, "/legal debería tener un único h1");
    assertTrue(html.contains("<h2"), "/legal debería estructurar sus secciones con h2");

    assertTrue(html.contains("href=\"#que-datos-guardamos\""), "debería enlazar sección de datos");
    assertTrue(html.contains("href=\"#uso-de-cookies\""), "debería enlazar sección de cookies");
    assertTrue(
        html.contains("href=\"#quien-puede-ver-que\""), "debería enlazar sección de visibilidad");
    assertTrue(
        html.contains("href=\"#conservacion-y-contacto\""),
        "debería enlazar sección de conservación y contacto");

    assertTrue(html.contains("donantes"), "debería detallar datos de donantes");
    assertTrue(
        html.contains("entidades beneficiarias"),
        "debería detallar datos de entidades beneficiarias");
    assertTrue(html.contains("donaciones"), "debería detallar datos de donaciones");
    assertTrue(html.contains("fotos"), "debería detallar el uso de fotos");
    assertTrue(html.contains("camiones"), "debería detallar las posiciones de camiones");

    assertTrue(html.contains("cookies"), "debería declarar el uso de cookies");
    assertTrue(html.contains("sesión"), "debería explicar que las cookies son de sesión");
    assertTrue(
        html.contains("sin iniciar sesión"), "debería aclarar qué datos se publican sin login");

    assertTrue(
        html.contains("mailto:solidaria@frba.utn.edu.ar"),
        "debería indicar el contacto para pedir baja o corrección");
    assertTrue(
        html.contains("Última actualización"), "debería mostrar la fecha de última actualización");

    assertTrue(html.contains("href=\"/\""), "debería enlazar de vuelta a la landing");
    assertTrue(html.contains("href=\"/login\""), "debería enlazar al login");
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
