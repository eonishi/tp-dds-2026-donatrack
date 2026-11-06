package ar.edu.utn.donatrack.testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.beneficiarios.Necesidad;
import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.notificaciones.AvisoBandeja;
import ar.edu.utn.donatrack.security.Usuario;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Arma escenarios para las pruebas de las vistas: altas por la API con la administración, el flujo
 * de una donación hasta que sale en camión y pedidos como los de un navegador (cookie, formularios
 * y subida de archivos).
 */
public final class EscenarioHttp implements WithSimplePersistenceUnit {
  public static final String CLAVE_SERVICIO = "clave-interna-de-prueba";
  public static final String PATENTE = "AF123ZZ";

  private final ObjectMapper mapper = new ObjectMapper();
  private final HttpClient client = HttpClient.newHttpClient();
  private final String baseUrl;
  private final String admin;

  public EscenarioHttp(String baseUrl) throws Exception {
    this.baseUrl = baseUrl;
    this.admin = login("admin@donatrack.local", "admin123");
  }

  public String admin() {
    return admin;
  }

  /** Borra lo que crearon las pruebas, del hijo al padre; conserva los usuarios semilla. */
  public void limpiar() {
    withTransaction(
        () -> {
          createQuery("select a from AvisoBandeja a", AvisoBandeja.class)
              .getResultList()
              .forEach(this::remove);
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

  // --- Altas ---

  public UUID crearDonante(String nombre, String documento, String email) throws Exception {
    String body =
        "{\"tipoPersona\":\"HUMANA\",\"numeroDocumento\":\""
            + documento
            + "\",\"tipoDocumento\":\"DNI\",\"mediosDeContacto\":[{\"tipo\":\"EMAIL\",\"contacto\":\""
            + email
            + "\"}],\"contactoPredeterminado\":0,\"nombre\":\""
            + nombre
            + "\",\"apellido\":\"Pérez\",\"genero\":\"FEMENINO\",\"direccion\":\"Calle 123\"}";
    HttpResponse<String> response = json("POST", "/api/v1/donantes", body, admin);
    assertEquals(201, response.statusCode(), response.body());
    return UUID.fromString(leer(response).get("id").asText());
  }

  public long crearEntidad(String razonSocial) throws Exception {
    HttpResponse<String> response =
        json(
            "POST",
            "/api/v1/beneficiarias",
            "{\"razonSocial\":\""
                + razonSocial
                + "\",\"direccion\":\"Calle 123\",\"telefono\":\"1111111111\",\"correos\":[\"entidad@example.org\"]}",
            admin);
    assertEquals(201, response.statusCode(), response.body());
    return Long.parseLong(leer(response).get("id").asText());
  }

  public void crearNecesidad(long entidad, String subcategoria) throws Exception {
    HttpResponse<String> response =
        json(
            "POST",
            "/api/v1/beneficiarias/" + entidad + "/necesidades",
            "{\"tipo\":\"EXTRAORDINARIA\",\"descripcion\":\"Alimentos\",\"cantidadObjetivo\":10,\"unidad\":\"KILOGRAMOS\",\"subcategoria\":\""
                + subcategoria
                + "\",\"categoria\":\"ALIMENTO\"}",
            admin);
    assertEquals(201, response.statusCode(), response.body());
  }

  public UUID crearDonacion(UUID donante, String subcategoria) throws Exception {
    return crearDonacion(donante, subcategoria, null);
  }

  /** Una donación de 10 kg; con {@code vence} (AAAA-MM-DD) es perecedera. */
  public UUID crearDonacion(UUID donante, String subcategoria, String vence) throws Exception {
    HttpResponse<String> response =
        json(
            "POST",
            "/api/v1/donaciones",
            "{\"donanteId\":\""
                + donante
                + "\",\"bienes\":[{\"descripcion\":\"Producto\",\"cantidad\":10,\"unidad\":\"KILOGRAMOS\",\"estadoDeUso\":\"NO_APLICA\","
                + (vence == null ? "" : "\"fechaVencimiento\":\"" + vence + "\",")
                + "\"subcategoria\":\""
                + subcategoria
                + "\",\"categoria\":\"ALIMENTO\"}]}",
            admin);
    assertEquals(201, response.statusCode(), response.body());
    return UUID.fromString(leer(response).get(0).get("id").asText());
  }

  /** Crea el usuario de una entidad y devuelve su cookie de sesión. */
  public String sesionDeEntidad(long entidad, String email) throws Exception {
    HttpResponse<String> response =
        json(
            "POST",
            "/api/v1/usuarios",
            "{\"email\":\""
                + email
                + "\",\"password\":\"clave1234\",\"rol\":\"BENEFICIARIO\",\"entidadId\":"
                + entidad
                + "}",
            admin);
    assertEquals(201, response.statusCode(), response.body());
    return login(email, "clave1234");
  }

  /** Crea el usuario de una persona donante y devuelve su cookie de sesión. */
  public String sesionDeDonante(UUID donante, String email) throws Exception {
    HttpResponse<String> response =
        json(
            "POST",
            "/api/v1/usuarios",
            "{\"email\":\""
                + email
                + "\",\"password\":\"clave1234\",\"rol\":\"DONANTE\",\"donanteId\":\""
                + donante
                + "\"}",
            admin);
    assertEquals(201, response.statusCode(), response.body());
    return login(email, "clave1234");
  }

  // --- Flujo hasta EN_TRASLADO, como lo hacen la administración y logística ---

  public void asignar(UUID donacion, long entidad) throws Exception {
    assertEquals(
        201,
        json("POST", "/api/v1/donaciones/" + donacion + "/propuesta-asignacion", null, admin)
            .statusCode());
    assertEquals(
        200,
        json(
                "PUT",
                "/api/v1/donaciones/" + donacion + "/propuesta-asignacion",
                "{\"entidadBeneficiariaId\":" + entidad + "}",
                admin)
            .statusCode());
  }

  public void llevarATraslado(UUID donacion, long entidad) throws Exception {
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

  public String estado(UUID donacion) throws Exception {
    HttpResponse<String> response = json("GET", "/api/v1/donaciones/" + donacion, null, admin);
    assertEquals(200, response.statusCode(), response.body());
    return leer(response).get("estado").asText();
  }

  private HttpResponse<String> cambioMasivo(String body) throws Exception {
    return client.send(
        HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/donaciones/estado"))
            .header("Authorization", CLAVE_SERVICIO)
            .header("Content-Type", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  // --- HTTP como un navegador ---

  public String login(String email, String password) throws Exception {
    HttpResponse<String> response =
        formulario("/login", Map.of("email", email, "password", password), "");
    String setCookie = response.headers().firstValue("Set-Cookie").orElseThrow();
    assertTrue(setCookie.startsWith("access_token="), "el login debe devolver la cookie");
    return setCookie.substring(0, setCookie.indexOf(';'));
  }

  public HttpResponse<String> get(String ruta, String cookie) throws Exception {
    return client.send(
        HttpRequest.newBuilder(URI.create(baseUrl + ruta)).header("Cookie", cookie).GET().build(),
        HttpResponse.BodyHandlers.ofString());
  }

  public HttpResponse<byte[]> getBytes(String ruta) throws Exception {
    return client.send(
        HttpRequest.newBuilder(URI.create(baseUrl + ruta)).GET().build(),
        HttpResponse.BodyHandlers.ofByteArray());
  }

  public HttpResponse<String> formulario(String ruta, Map<String, String> campos, String cookie)
      throws Exception {
    StringBuilder body = new StringBuilder();
    campos.forEach(
        (clave, valor) -> {
          if (body.length() > 0) {
            body.append('&');
          }
          body.append(URLEncoder.encode(clave, StandardCharsets.UTF_8))
              .append('=')
              .append(URLEncoder.encode(valor, StandardCharsets.UTF_8));
        });
    return client.send(
        HttpRequest.newBuilder(URI.create(baseUrl + ruta))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Cookie", cookie)
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  /** Un archivo para {@link #subir}. */
  public record Archivo(String campo, String nombre, String tipo, byte[] contenido) {}

  /** POST multipart/form-data, como un formulario con {@code <input type="file">}. */
  public HttpResponse<String> subir(String ruta, List<Archivo> archivos, String cookie)
      throws Exception {
    String limite = "----donatrack" + UUID.randomUUID();
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    for (Archivo archivo : archivos) {
      body.write(
          ("--"
                  + limite
                  + "\r\nContent-Disposition: form-data; name=\""
                  + archivo.campo()
                  + "\"; filename=\""
                  + archivo.nombre()
                  + "\"\r\nContent-Type: "
                  + archivo.tipo()
                  + "\r\n\r\n")
              .getBytes(StandardCharsets.UTF_8));
      body.write(archivo.contenido());
      body.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }
    body.write(("--" + limite + "--\r\n").getBytes(StandardCharsets.UTF_8));
    return client.send(
        HttpRequest.newBuilder(URI.create(baseUrl + ruta))
            .header("Content-Type", "multipart/form-data; boundary=" + limite)
            .header("Cookie", cookie)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  public HttpResponse<String> json(String method, String path, String body, String cookie)
      throws IOException, InterruptedException {
    return client.send(
        HttpRequest.newBuilder(URI.create(baseUrl + path))
            .header("Content-Type", "application/json")
            .header("Cookie", cookie)
            .method(
                method,
                body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  public JsonNode leer(HttpResponse<String> response) throws IOException {
    return mapper.readTree(response.body());
  }

  /** Los primeros bytes de un PNG: alcanza para que el sistema lo reconozca como imagen. */
  public static byte[] png() {
    return new byte[] {
      (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'
    };
  }
}
