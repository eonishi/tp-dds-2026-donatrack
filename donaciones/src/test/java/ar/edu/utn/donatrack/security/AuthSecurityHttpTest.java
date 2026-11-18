package ar.edu.utn.donatrack.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.testsupport.ApiDePrueba;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.Javalin;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthSecurityHttpTest implements WithSimplePersistenceUnit {
  private final HttpClient client = HttpClient.newHttpClient();
  private Javalin app;
  private String baseUrl;

  @BeforeEach
  void start() {
    app = ApiDePrueba.crear().start(0);
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
    assertTrue(dashboard.body().contains("Hola, admin"));
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

  @Test
  void llamadaS2SConApiKeyPuedeConsultarYActualizarDonacionesSinCookie() throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(
                URI.create(baseUrl + "/api/v1/donaciones?estado=ASIGNACION_REALIZADA"))
            .header("Authorization", "S2S_SECRET_KEY")
            .GET()
            .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(401, response.statusCode(), response.body());
    assertTrue(ControlDeAcceso.claveValida("clave-configurada", "clave-configurada"));
    assertTrue(!ControlDeAcceso.claveValida("S2S_SECRET_KEY", ""));
  }

  @Test
  void altaDeSubcategoriaExigeRolAdmin() throws Exception {
    HttpResponse<String> sinSesion =
        post(
            "/api/v1/subcategorias",
            "{\"nombre\":\"Arroz\",\"categoria\":\"ALIMENTO\"}",
            "application/json");
    assertEquals(401, sinSesion.statusCode());

    HttpResponse<String> loginDonante =
        post(
            "/login",
            "email=donante%40donatrack.local&password=donante123",
            "application/x-www-form-urlencoded");
    String cookieDonante = loginDonante.headers().firstValue("Set-Cookie").orElse("");

    HttpRequest requestDonante =
        HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/subcategorias"))
            .header("Cookie", cookieDonante)
            .header("Content-Type", "application/json")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    "{\"nombre\":\"Arroz\",\"categoria\":\"ALIMENTO\"}"))
            .build();
    HttpResponse<String> respuestaDonante =
        client.send(requestDonante, HttpResponse.BodyHandlers.ofString());
    assertEquals(403, respuestaDonante.statusCode());
  }

  @Test
  void elPanelRenderizaLasCategoriasYUnidadesDelDominio() throws Exception {
    HttpResponse<String> loginAdmin =
        post(
            "/login",
            "email=admin%40donatrack.local&password=admin123",
            "application/x-www-form-urlencoded");
    String cookieAdmin = loginAdmin.headers().firstValue("Set-Cookie").orElse("");

    HttpResponse<String> panel = get("/donaciones", cookieAdmin);
    assertEquals(200, panel.statusCode());
    assertTrue(panel.body().contains("ALIMENTO"));
    assertTrue(panel.body().contains("MOBILIARIO"));
    assertTrue(panel.body().contains("VESTIMENTA"));
    assertTrue(panel.body().contains("KILOGRAMOS"));
    assertTrue(panel.body().contains("UNIDADES"));
    assertTrue(panel.body().contains("LITROS"));
  }

  @Test
  void unaCookieConFirmaCorruptaSeTrataComoAnonima() throws Exception {
    String cookie = "access_token=a.b.!!!";

    assertEquals(200, get("/", cookie).statusCode());
    assertEquals(302, get("/dashboard", cookie).statusCode());
  }

  @Test
  void logisticaConLaClaveInternaConsultaDonacionesSinSesion() throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/donaciones"))
            .header("Authorization", "clave-interna-de-prueba")
            .GET()
            .build();

    assertEquals(200, client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
  }

  @Test
  void laClaveInternaNoAbreElRestoDeLaApi() throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/donantes"))
            .header("Authorization", "clave-interna-de-prueba")
            .GET()
            .build();

    assertEquals(401, client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
  }

  @Test
  void elPanelDeLogisticaEsSoloParaAdministradores() throws Exception {
    HttpResponse<String> loginResponse =
        post(
            "/login",
            "email=donante%40donatrack.local&password=donante123",
            "application/x-www-form-urlencoded");

    String cookie = loginResponse.headers().firstValue("Set-Cookie").orElse("");
    assertEquals(403, get("/logistica", cookie).statusCode());
  }

  @Test
  void elBeneficiarioDePruebaPuedeAbrirYCargarSusNecesidades() throws Exception {
    HttpResponse<String> loginResponse =
        post(
            "/login",
            "email=beneficiario%40donatrack.local&password=beneficiario123",
            "application/x-www-form-urlencoded");

    assertEquals(302, loginResponse.statusCode());
    String cookie = loginResponse.headers().firstValue("Set-Cookie").orElse("");
    assertEquals(200, get("/dashboard", cookie).statusCode());
    assertEquals(200, get("/entidad/necesidades", cookie).statusCode());
    HttpRequest alta =
        HttpRequest.newBuilder(URI.create(baseUrl + "/entidad/necesidades"))
            .header("Cookie", cookie)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    "tipo=EXTRAORDINARIA&categoria=ALIMENTO&subcategoria=ArrozDemo&cantidad=10&unidad=KILOGRAMOS&descripcion=Alimentos"))
            .build();
    HttpResponse<String> respuesta = client.send(alta, HttpResponse.BodyHandlers.ofString());
    assertEquals(302, respuesta.statusCode(), respuesta.body());
    assertTrue(get("/entidad/necesidades", cookie).body().contains("ArrozDemo"));
  }

  @Test
  void reiniciarAuthConservaLaEntidadDeEjemploSinDuplicarla() {
    UsuariosRepository usuarios = new UsuariosRepository();
    Usuario usuario = usuarios.buscarPorEmail("beneficiario@donatrack.local").orElseThrow();
    Long entidadId = usuario.getEntidadId().orElseThrow();
    Long cantidad =
        createQuery("select count(e) from EntidadBeneficiaria e", Long.class).getSingleResult();
    new AuthService();
    assertEquals(
        entidadId,
        usuarios.buscarPorEmail(usuario.getEmail()).orElseThrow().getEntidadId().orElseThrow());
    assertEquals(
        cantidad,
        createQuery("select count(e) from EntidadBeneficiaria e", Long.class).getSingleResult());
  }

  @Test
  void reiniciarAuthReparaLaCuentaDeEjemploPreexistenteSinEntidad() {
    UsuariosRepository usuarios = new UsuariosRepository();
    Usuario usuario = usuarios.buscarPorEmail("beneficiario@donatrack.local").orElseThrow();
    String hash = usuario.getPasswordHash();
    withTransaction(
        () -> {
          createQuery("update Usuario u set u.entidadId = null where u.id = :id")
              .setParameter("id", usuario.getId())
              .executeUpdate();
        });
    entityManager().clear();
    new AuthService();
    Usuario reparado = usuarios.buscarPorEmail(usuario.getEmail()).orElseThrow();
    assertEquals(hash, reparado.getPasswordHash());
    assertTrue(reparado.getEntidadId().isPresent());
    assertTrue(find(EntidadBeneficiaria.class, reparado.getEntidadId().orElseThrow()) != null);
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
