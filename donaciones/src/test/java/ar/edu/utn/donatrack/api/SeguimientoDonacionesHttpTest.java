package ar.edu.utn.donatrack.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.beneficiarios.Necesidad;
import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.security.Usuario;
import ar.edu.utn.donatrack.seguimiento.PosicionCamion;
import ar.edu.utn.donatrack.testsupport.ApiDePrueba;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.Javalin;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * La persona donante y la entidad beneficiaria ven solo sus donaciones y siguen el camión que las
 * lleva. Logística se reemplaza por una posición fija.
 */
class SeguimientoDonacionesHttpTest implements WithSimplePersistenceUnit {
  private static final String CLAVE_SERVICIO = "clave-interna-de-prueba";
  private static final String PATENTE = "AF123ZZ";

  private final ObjectMapper mapper = new ObjectMapper();
  private final HttpClient client = HttpClient.newHttpClient();
  private Javalin app;
  private String baseUrl;
  private String admin;

  @BeforeEach
  void start() throws Exception {
    Clock clock = Clock.fixed(Instant.parse("2026-09-11T12:00:00Z"), ZoneOffset.UTC);
    app =
        ApiDePrueba.crear(
                clock,
                patente ->
                    PATENTE.equals(patente)
                        ? Optional.of(
                            new PosicionCamion(patente, -34.6, -58.4, "2026-09-11T11:59:00"))
                        : Optional.empty())
            .start(0);
    baseUrl = "http://localhost:" + app.port();
    admin = login("admin@donatrack.local", "admin123");
  }

  @AfterEach
  void stop() {
    app.stop();
  }

  // Mismo orden de borrado que DonacionesApiHttpTest (del hijo al padre), más los usuarios que dio
  // de alta cada prueba. Los de prueba del sistema (@donatrack.local) se conservan.
  @AfterEach
  void limpiarPersistencia() {
    withTransaction(
        () -> {
          createQuery("select b from Bien b", Bien.class).getResultList().forEach(this::remove);
          createQuery("select n from Necesidad n", Necesidad.class)
              .getResultList()
              .forEach(this::remove);
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
  void laDonanteVeSoloSusDonacionesYSigueElCamion() throws Exception {
    UUID sofia = crearDonante("Sofía", "30111222", "sofia@example.org");
    UUID otra = crearDonante("Lucía", "30999888", "lucia@example.org");
    long entidad = crearEntidad("Comedor Seguimiento");
    crearNecesidad(entidad, "Arroz");
    UUID deSofia = crearDonacion(sofia, "Arroz");
    UUID deOtra = crearDonacion(otra, "Arroz");
    llevarATraslado(deSofia, entidad);

    crearUsuario(
        "{\"email\":\"sofia@example.org\",\"password\":\"sofia1234\",\"rol\":\"DONANTE\",\"donanteId\":\""
            + sofia
            + "\"}");
    String cookieSofia = login("sofia@example.org", "sofia1234");

    HttpResponse<String> lista = request("GET", "/api/v1/donaciones", null, cookieSofia);
    assertEquals(200, lista.statusCode());
    assertEquals(1, json(lista).size());
    assertEquals(deSofia.toString(), json(lista).get(0).get("id").asText());

    assertEquals(
        404, request("GET", "/api/v1/donaciones/" + deOtra, null, cookieSofia).statusCode());

    HttpResponse<String> seguimiento =
        request("GET", "/api/v1/donaciones/" + deSofia + "/seguimiento", null, cookieSofia);
    assertEquals(200, seguimiento.statusCode(), seguimiento.body());
    assertEquals(PATENTE, json(seguimiento).get("patente").asText());
    assertEquals(-34.6, json(seguimiento).get("latitud").asDouble());
  }

  @Test
  void laEntidadSigueElCamionYConfirmaLaRecepcionDeLoQueLeAsignaron() throws Exception {
    UUID donante = crearDonante("Sofía", "30111222", "sofia@example.org");
    long entidad = crearEntidad("Comedor Seguimiento");
    crearNecesidad(entidad, "Arroz");
    UUID donacion = crearDonacion(donante, "Arroz");
    llevarATraslado(donacion, entidad);

    crearUsuario(
        "{\"email\":\"comedor@example.org\",\"password\":\"comedor123\",\"rol\":\"BENEFICIARIO\",\"entidadId\":"
            + entidad
            + "}");
    String cookieEntidad = login("comedor@example.org", "comedor123");

    assertEquals(
        200,
        request("GET", "/api/v1/donaciones/" + donacion + "/seguimiento", null, cookieEntidad)
            .statusCode());
    assertEquals(
        403,
        request(
                "PATCH",
                "/api/v1/donaciones/" + donacion + "/estado",
                "{\"estado\":\"VENCIDA\"}",
                cookieEntidad)
            .statusCode());
    HttpResponse<String> recepcion =
        request(
            "PATCH",
            "/api/v1/donaciones/" + donacion + "/estado",
            "{\"estado\":\"ENTREGADA\"}",
            cookieEntidad);
    assertEquals(200, recepcion.statusCode(), recepcion.body());
    assertEquals("ENTREGADA", json(recepcion).get("estado").asText());
  }

  @Test
  void cualquieraVeLasEntregasConFotosSinIniciarSesionNiDatosDeLaDonante() throws Exception {
    UUID donante = crearDonante("Sofía", "30111222", "sofia@example.org");
    long entidad = crearEntidad("Comedor Seguimiento");
    crearNecesidad(entidad, "Arroz");
    UUID entregada = crearDonacion(donante, "Arroz");
    llevarATraslado(entregada, entidad);
    assertEquals(
        200,
        request(
                "PATCH",
                "/api/v1/donaciones/" + entregada + "/estado",
                "{\"estado\":\"ENTREGADA\"}",
                admin)
            .statusCode());
    assertEquals(
        201,
        request(
                "POST",
                "/api/v1/donaciones/" + entregada + "/fotos",
                "{\"url\":\"https://fotos.example.org/entrega-1.jpg\"}",
                admin)
            .statusCode());

    HttpResponse<String> pagina = request("GET", "/entregas", null, "");

    assertEquals(200, pagina.statusCode());
    String html = pagina.body();
    assertTrue(html.contains("Comedor Seguimiento"), "debe mostrar la entidad");
    assertTrue(html.contains("https://fotos.example.org/entrega-1.jpg"), "debe mostrar la foto");
    assertTrue(html.contains("10 kilogramos de Arroz"), "debe mostrar qué se entregó");
    assertTrue(
        !html.contains("sofia@example.org") && !html.contains("30111222"),
        "no debe publicar datos de la donante");
  }

  @Test
  void laDonanteNoModificaDonacionesNiSigueLasQueNoEstanEnTraslado() throws Exception {
    UUID sofia = crearDonante("Sofía", "30111222", "sofia@example.org");
    UUID donacion = crearDonacion(sofia, "Arroz");
    crearUsuario(
        "{\"email\":\"sofia@example.org\",\"password\":\"sofia1234\",\"rol\":\"DONANTE\",\"donanteId\":\""
            + sofia
            + "\"}");
    String cookieSofia = login("sofia@example.org", "sofia1234");

    assertEquals(
        409,
        request("GET", "/api/v1/donaciones/" + donacion + "/seguimiento", null, cookieSofia)
            .statusCode());
    assertEquals(
        403, request("DELETE", "/api/v1/donaciones/" + donacion, null, cookieSofia).statusCode());
  }

  @Test
  void logisticaFiltraLasDonacionesPorEstadoConLaClaveInterna() throws Exception {
    UUID donante = crearDonante("Sofía", "30111222", "sofia@example.org");
    long entidad = crearEntidad("Comedor Seguimiento");
    crearNecesidad(entidad, "Arroz");
    UUID asignada = crearDonacion(donante, "Arroz");
    crearDonacion(donante, "Arroz");
    asignar(asignada, entidad);

    HttpRequest pedido =
        HttpRequest.newBuilder(
                URI.create(baseUrl + "/api/v1/donaciones?estado=ASIGNACION_REALIZADA"))
            .header("Authorization", CLAVE_SERVICIO)
            .GET()
            .build();
    HttpResponse<String> response = client.send(pedido, HttpResponse.BodyHandlers.ofString());

    assertEquals(200, response.statusCode());
    assertEquals(1, json(response).size());
    assertEquals(asignada.toString(), json(response).get(0).get("id").asText());
  }

  @Test
  void elAltaDeUsuariosEsSoloDeLaAdministracionYExigeElVinculo() throws Exception {
    String donanteSemilla = login("donante@donatrack.local", "donante123");
    String alta = "{\"email\":\"x@example.org\",\"password\":\"clave1234\",\"rol\":\"DONANTE\"}";

    assertEquals(403, request("POST", "/api/v1/usuarios", alta, donanteSemilla).statusCode());
    assertEquals(400, request("POST", "/api/v1/usuarios", alta, admin).statusCode());
  }

  // --- Flujo hasta EN_TRASLADO, como lo hacen la administración y logística ---

  private void asignar(UUID donacion, long entidad) throws Exception {
    assertEquals(
        201,
        request("POST", "/api/v1/donaciones/" + donacion + "/propuesta-asignacion", null, admin)
            .statusCode());
    assertEquals(
        200,
        request(
                "PUT",
                "/api/v1/donaciones/" + donacion + "/propuesta-asignacion",
                "{\"entidadBeneficiariaId\":" + entidad + "}",
                admin)
            .statusCode());
  }

  private void llevarATraslado(UUID donacion, long entidad) throws Exception {
    asignar(donacion, entidad);
    assertEquals(
        200,
        cambioMasivo(
                "[{\"idDonacion\":\"" + donacion + "\",\"nuevoEstado\":\"LISTA_PARA_ENTREGAR\"}]")
            .statusCode());
    HttpResponse<String> inicio =
        cambioMasivo(
            "[{\"idDonacion\":\""
                + donacion
                + "\",\"nuevoEstado\":\"EN_TRASLADO\",\"idCamion\":\""
                + PATENTE
                + "\"}]");
    assertEquals(200, inicio.statusCode(), inicio.body());
  }

  // Lo que hace logística al iniciar una ruta: PATCH masivo autenticado con la clave interna.
  private HttpResponse<String> cambioMasivo(String body) throws Exception {
    HttpRequest pedido =
        HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/donaciones/estado"))
            .header("Authorization", CLAVE_SERVICIO)
            .header("Content-Type", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
            .build();
    return client.send(pedido, HttpResponse.BodyHandlers.ofString());
  }

  // --- Altas con la administración ---

  private void crearUsuario(String body) throws Exception {
    HttpResponse<String> response = request("POST", "/api/v1/usuarios", body, admin);
    assertEquals(201, response.statusCode(), response.body());
  }

  private UUID crearDonante(String nombre, String documento, String email) throws Exception {
    String body =
        "{\"tipoPersona\":\"HUMANA\",\"numeroDocumento\":\""
            + documento
            + "\",\"tipoDocumento\":\"DNI\",\"mediosDeContacto\":[{\"tipo\":\"EMAIL\",\"contacto\":\""
            + email
            + "\"}],\"contactoPredeterminado\":0,\"nombre\":\""
            + nombre
            + "\",\"apellido\":\"Pérez\",\"edad\":30,\"genero\":\"FEMENINO\",\"direccion\":\"Calle 123\"}";
    HttpResponse<String> response = request("POST", "/api/v1/donantes", body, admin);
    assertEquals(201, response.statusCode(), response.body());
    return UUID.fromString(json(response).get("id").asText());
  }

  private long crearEntidad(String razonSocial) throws Exception {
    HttpResponse<String> response =
        request(
            "POST",
            "/api/v1/beneficiarias",
            "{\"razonSocial\":\""
                + razonSocial
                + "\",\"direccion\":\"Calle 123\",\"telefono\":\"1111111111\",\"correos\":[\"entidad@example.org\"]}",
            admin);
    assertEquals(201, response.statusCode(), response.body());
    return Long.parseLong(json(response).get("id").asText());
  }

  private void crearNecesidad(long entidad, String subcategoria) throws Exception {
    HttpResponse<String> response =
        request(
            "POST",
            "/api/v1/beneficiarias/" + entidad + "/necesidades",
            "{\"tipo\":\"EXTRAORDINARIA\",\"descripcion\":\"Alimentos\",\"cantidadObjetivo\":10,\"unidad\":\"KILOGRAMOS\",\"subcategoria\":\""
                + subcategoria
                + "\",\"categoria\":\"ALIMENTO\"}",
            admin);
    assertEquals(201, response.statusCode(), response.body());
  }

  private UUID crearDonacion(UUID donante, String subcategoria) throws Exception {
    HttpResponse<String> response =
        request(
            "POST",
            "/api/v1/donaciones",
            "{\"donanteId\":\""
                + donante
                + "\",\"bienes\":[{\"descripcion\":\"Producto\",\"cantidad\":10,\"unidad\":\"KILOGRAMOS\",\"estadoDeUso\":\"NO_APLICA\",\"subcategoria\":\""
                + subcategoria
                + "\",\"categoria\":\"ALIMENTO\"}]}",
            admin);
    assertEquals(201, response.statusCode(), response.body());
    return UUID.fromString(json(response).get(0).get("id").asText());
  }

  // --- HTTP ---

  private String login(String email, String password) throws IOException, InterruptedException {
    HttpRequest pedido =
        HttpRequest.newBuilder(URI.create(baseUrl + "/login"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    "email=" + email.replace("@", "%40") + "&password=" + password))
            .build();
    HttpResponse<String> response = client.send(pedido, HttpResponse.BodyHandlers.ofString());
    String setCookie = response.headers().firstValue("Set-Cookie").orElseThrow();
    assertTrue(setCookie.startsWith("access_token="), "el login debe devolver la cookie");
    return setCookie.substring(0, setCookie.indexOf(';'));
  }

  private HttpResponse<String> request(String method, String path, String body, String cookie)
      throws IOException, InterruptedException {
    HttpRequest pedido =
        HttpRequest.newBuilder(URI.create(baseUrl + path))
            .header("Content-Type", "application/json")
            .header("Cookie", cookie)
            .method(
                method,
                body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body))
            .build();
    return client.send(pedido, HttpResponse.BodyHandlers.ofString());
  }

  private JsonNode json(HttpResponse<String> response) throws IOException {
    return mapper.readTree(response.body());
  }
}
