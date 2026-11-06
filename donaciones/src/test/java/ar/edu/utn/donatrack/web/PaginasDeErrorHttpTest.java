package ar.edu.utn.donatrack.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.donatrack.testsupport.ApiDePrueba;
import io.javalin.Javalin;
import io.javalin.http.MethodNotAllowedResponse;
import io.javalin.http.UnauthorizedResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Páginas de error propias dentro del layout, sin tocar las respuestas JSON de la API. */
class PaginasDeErrorHttpTest {
  private final HttpClient client = HttpClient.newHttpClient();
  private Javalin app;
  private String baseUrl;

  @BeforeEach
  void start() {
    app =
        ApiDePrueba.crear()
            .get(
                "/prueba-de-error",
                ctx -> {
                  throw new RuntimeException("detalle interno que no se debe mostrar");
                })
            .get(
                "/prueba-de-dato-invalido",
                ctx -> {
                  throw new IllegalArgumentException("detalle interno");
                })
            .get(
                "/prueba-de-sesion",
                ctx -> {
                  throw new UnauthorizedResponse("detalle interno");
                })
            .get(
                "/prueba-de-metodo",
                ctx -> {
                  throw new MethodNotAllowedResponse("detalle interno");
                })
            .get(
                "/prueba-de-conflicto",
                ctx -> {
                  throw new IllegalStateException("detalle interno");
                })
            .start(0);
    baseUrl = "http://localhost:" + app.port();
  }

  @AfterEach
  void stop() {
    app.stop();
  }

  @Test
  void unaPaginaInexistenteMuestraEl404ConElLayoutYLaVueltaAlInicio() throws Exception {
    HttpResponse<String> response = get("/esta-pagina-no-existe", "");

    assertEquals(404, response.statusCode());
    assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("text/html"));
    String html = response.body();
    assertTrue(html.contains("<nav"), "debería incluir el layout");
    assertTrue(html.contains("No encontramos esta página"));
    assertTrue(html.contains("href=\"/\""), "debería permitir volver al inicio");
  }

  @Test
  void unaSeccionSinPermisoMuestraEl403() throws Exception {
    String donante = login("donante@donatrack.local", "donante123");

    HttpResponse<String> response = get("/logistica", donante);

    assertEquals(403, response.statusCode());
    assertTrue(response.body().contains("No tenés acceso a esta sección"));
    assertTrue(response.body().contains("href=\"/dashboard\""), "debería ofrecer volver al panel");
  }

  @Test
  void unErrorInesperadoMuestraEl500SinElDetalleInterno() throws Exception {
    HttpResponse<String> response = get("/prueba-de-error", "");

    assertEquals(500, response.statusCode());
    assertTrue(response.body().contains("Algo salió mal"));
    assertFalse(response.body().contains("detalle interno"), "no debería exponer la excepción");
  }

  @Test
  void losDemasErroresTambienTienenSuPagina() throws Exception {
    String[][] casos = {
      {"/prueba-de-dato-invalido", "400", "No pudimos procesar el pedido"},
      {"/prueba-de-sesion", "401", "Tenés que iniciar sesión"},
      {"/prueba-de-metodo", "405", "Esta acción no está disponible"},
      {"/prueba-de-conflicto", "409", "No se pudo completar la acción"}
    };

    for (String[] caso : casos) {
      HttpResponse<String> response = get(caso[0], "");

      assertEquals(Integer.parseInt(caso[1]), response.statusCode(), caso[0]);
      assertTrue(response.body().contains("<nav"), caso[0] + " debería incluir el layout");
      assertTrue(response.body().contains(caso[2]), caso[0]);
      assertFalse(response.body().contains("detalle interno"), caso[0] + " expone la excepción");
    }
  }

  @Test
  void unaVistaQueYaInformaSuErrorNoSeReemplaza() throws Exception {
    HttpResponse<String> response =
        client.send(
            HttpRequest.newBuilder(URI.create(baseUrl + "/registro"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("email=x%40y.com&password=12345678"))
                .build(),
            HttpResponse.BodyHandlers.ofString());

    assertEquals(400, response.statusCode());
    assertFalse(response.body().contains("No pudimos procesar el pedido"));
    assertTrue(response.body().contains("<form"), "debería seguir mostrando el formulario");
  }

  @Test
  void laApiSigueRespondiendoSusErroresEnJson() throws Exception {
    HttpResponse<String> response = get("/api/v1/esta-ruta-no-existe", "");

    assertEquals(404, response.statusCode());
    assertTrue(response.body().contains("ROUTE_NOT_FOUND"));
    assertFalse(response.body().contains("<nav"));
  }

  @Test
  void todasLasSeccionesDelNavbarRespondenSinErrores() throws Exception {
    String admin = login("admin@donatrack.local", "admin123");
    String[] secciones = {
      "/",
      "/entregas",
      "/legal",
      "/registro",
      "/dashboard",
      "/perfil",
      "/donaciones",
      "/necesidades",
      "/logistica",
      "/matchmaking",
      "/catalogo",
      "/admin/vencidas",
      "/admin/asignaciones",
      "/admin/camiones",
      "/admin/importar",
      "/activar"
    };

    for (String seccion : secciones) {
      assertEquals(200, get(seccion, admin).statusCode(), seccion);
    }
  }

  private String login(String email, String password) throws Exception {
    HttpResponse<String> response =
        client.send(
            HttpRequest.newBuilder(URI.create(baseUrl + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        "email=" + email.replace("@", "%40") + "&password=" + password))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    String cookie = response.headers().firstValue("Set-Cookie").orElseThrow();
    return cookie.substring(0, cookie.indexOf(';'));
  }

  private HttpResponse<String> get(String ruta, String cookie) throws Exception {
    return client.send(
        HttpRequest.newBuilder(URI.create(baseUrl + ruta)).header("Cookie", cookie).GET().build(),
        HttpResponse.BodyHandlers.ofString());
  }
}
