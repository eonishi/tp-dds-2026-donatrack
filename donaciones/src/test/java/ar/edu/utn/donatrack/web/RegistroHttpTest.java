package ar.edu.utn.donatrack.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.security.Usuario;
import ar.edu.utn.donatrack.testsupport.ApiDePrueba;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.Javalin;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** El registro crea la persona donante o la entidad junto con su usuario ya vinculado. */
class RegistroHttpTest implements WithSimplePersistenceUnit {
  private final ObjectMapper mapper = new ObjectMapper();
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

  @AfterEach
  void limpiarPersistencia() {
    withTransaction(
        () -> {
          createQuery("select b from Bien b", Bien.class).getResultList().forEach(this::remove);
          createQuery("select d from Donacion d", Donacion.class)
              .getResultList()
              .forEach(this::remove);
          createQuery("select e from EntidadBeneficiaria e", EntidadBeneficiaria.class)
              .getResultList()
              .forEach(this::remove);
          createQuery("select d from Donante d", Donante.class)
              .getResultList()
              .forEach(this::remove);
          createQuery("select s from SubCategoria s", SubCategoria.class)
              .getResultList()
              .forEach(this::remove);
          createQuery(
                  "select u from Usuario u where u.email not like '%@donatrack.local'",
                  Usuario.class)
              .getResultList()
              .forEach(this::remove);
        });
  }

  @Test
  void laDonanteSeRegistraQuedaLogueadaYVeSoloSusDonaciones() throws Exception {
    HttpResponse<String> registro = registrar(donante("ana@example.org", "30111222"));

    assertEquals(302, registro.statusCode(), registro.body());
    assertEquals("/dashboard", registro.headers().firstValue("Location").orElse(""));
    String cookieAna = cookie(registro);

    String admin = cookie(login("admin@donatrack.local", "admin123"));
    String donanteId = buscarDonante(admin, "30111222").get("id").asText();
    assertEquals(201, crearDonacion(admin, donanteId).statusCode());

    HttpResponse<String> deAna = get("/api/v1/donaciones", cookieAna);
    assertEquals(200, deAna.statusCode());
    assertEquals(1, mapper.readTree(deAna.body()).size(), "ve la donación que se le cargó");
    assertEquals(
        302, login("ana@example.org", "clave-segura").statusCode(), "puede volver a entrar");
  }

  @Test
  void laEntidadSeRegistraYQuedaLogueada() throws Exception {
    Map<String, String> campos = comunes("comedor@example.org");
    campos.put("rol", "entidad");
    campos.put("entidadRazonSocial", "Comedor Los Pibes");
    campos.put("entidadDireccion", "Calle 123");
    campos.put("entidadTelefono", "1155555555");

    HttpResponse<String> registro = registrar(campos);

    assertEquals(302, registro.statusCode(), registro.body());
    assertEquals(200, get("/dashboard", cookie(registro)).statusCode());
  }

  @Test
  void noSePuedeRegistrarDosVecesElMismoCorreo() throws Exception {
    assertEquals(302, registrar(donante("ana@example.org", "30111222")).statusCode());

    HttpResponse<String> repetido = registrar(donante("ana@example.org", "30999888"));

    assertEquals(400, repetido.statusCode());
    assertTrue(repetido.body().contains("Ya hay una cuenta con ese correo electrónico."));
  }

  @Test
  void lasContraseniasTienenQueCoincidir() throws Exception {
    Map<String, String> campos = donante("ana@example.org", "30111222");
    campos.put("contrasenaRepetida", "otra-clave");

    HttpResponse<String> registro = registrar(campos);

    assertEquals(400, registro.statusCode());
    assertTrue(registro.body().contains("Las contraseñas no coinciden."));
  }

  private Map<String, String> comunes(String correo) {
    Map<String, String> campos = new LinkedHashMap<>();
    campos.put("correo", correo);
    campos.put("contrasena", "clave-segura");
    campos.put("contrasenaRepetida", "clave-segura");
    campos.put("terminos", "acepto");
    return campos;
  }

  private Map<String, String> donante(String correo, String documento) {
    Map<String, String> campos = comunes(correo);
    campos.put("rol", "donante");
    campos.put("donanteNombre", "Ana");
    campos.put("donanteApellido", "Gómez");
    campos.put("donanteTipoDocumento", "DNI");
    campos.put("donanteNumeroDocumento", documento);
    campos.put("donanteGenero", "FEMENINO");
    campos.put("donanteDireccion", "Calle 123");
    campos.put("donanteMedioContacto", "CORREO");
    return campos;
  }

  private JsonNode buscarDonante(String cookie, String documento) throws Exception {
    for (JsonNode donante : mapper.readTree(get("/api/v1/donantes", cookie).body())) {
      if (documento.equals(donante.get("numeroDocumento").asText())) {
        return donante;
      }
    }
    throw new AssertionError("no se creó la persona donante " + documento);
  }

  private HttpResponse<String> crearDonacion(String cookie, String donanteId) throws Exception {
    String body =
        "{\"donanteId\":\""
            + donanteId
            + "\",\"bienes\":[{\"descripcion\":\"Arroz\",\"cantidad\":5,\"unidad\":\"KILOGRAMOS\","
            + "\"estadoDeUso\":\"NO_APLICA\",\"subcategoria\":\"Arroz\",\"categoria\":\"ALIMENTO\"}]}";
    return client.send(
        HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/donaciones"))
            .header("Content-Type", "application/json")
            .header("Cookie", cookie)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> registrar(Map<String, String> campos) throws Exception {
    return postFormulario("/registro", campos);
  }

  private HttpResponse<String> login(String email, String password) throws Exception {
    return postFormulario("/login", Map.of("email", email, "password", password));
  }

  private HttpResponse<String> postFormulario(String ruta, Map<String, String> campos)
      throws Exception {
    String body =
        campos.entrySet().stream()
            .map(
                e ->
                    URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                        + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));
    return client.send(
        HttpRequest.newBuilder(URI.create(baseUrl + ruta))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> get(String ruta, String cookie) throws Exception {
    return client.send(
        HttpRequest.newBuilder(URI.create(baseUrl + ruta)).header("Cookie", cookie).GET().build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private static String cookie(HttpResponse<String> response) {
    String setCookie = response.headers().firstValue("Set-Cookie").orElseThrow();
    return setCookie.substring(0, setCookie.indexOf(';'));
  }
}
