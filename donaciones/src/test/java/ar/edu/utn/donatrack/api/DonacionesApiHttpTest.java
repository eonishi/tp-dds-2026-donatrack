package ar.edu.utn.donatrack.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DonacionesApiHttpTest {
  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
  private final HttpClient client = HttpClient.newHttpClient();
  private Javalin app;
  private String baseUrl;

  @BeforeEach
  void start() {
    Clock clock = Clock.fixed(Instant.parse("2026-09-11T12:00:00Z"), ZoneOffset.UTC);
    app = DonacionesApi.create(clock).start(0);
    baseUrl = "http://localhost:" + app.port();
  }

  @AfterEach
  void stop() {
    app.stop();
  }

  @Test
  void crudsCompletosRespetanCodigosHttpYErrores() throws Exception {
    UUID donanteId = crearDonante();
    assertEquals(200, request("GET", "/api/v1/donantes", null).statusCode());
    assertEquals(
        200, request("GET", "/api/v1/donantes/" + donanteId, null).statusCode());

    HttpResponse<String> donanteActualizado =
        request(
            "PUT",
            "/api/v1/donantes/" + donanteId,
            donanteJson("Ana", "Gómez", "ana.gomez@example.org"));
    assertEquals(200, donanteActualizado.statusCode());
    assertEquals("Ana", json(donanteActualizado).get("nombre").asText());

    UUID entidadId = crearEntidad("Comedor Central");
    assertEquals(200, request("GET", "/api/v1/beneficiarias", null).statusCode());
    HttpResponse<String> entidadActualizada =
        request(
            "PUT",
            "/api/v1/beneficiarias/" + entidadId,
            """
            {"razonSocial":"Comedor Central","direccion":"Nueva 456","telefono":"1122223333","correos":["contacto@comedor.org"]}
            """);
    assertEquals(200, entidadActualizada.statusCode());

    UUID necesidadId = crearNecesidad(entidadId, "Fideos");
    assertEquals(200, request("GET", "/api/v1/necesidades", null).statusCode());
    HttpResponse<String> necesidadActualizada =
        request(
            "PUT",
            "/api/v1/necesidades/" + necesidadId,
            necesidadJson(entidadId, "Fideos", 20));
    assertEquals(200, necesidadActualizada.statusCode());
    assertEquals(20, json(necesidadActualizada).get("cantidadObjetivo").asInt());

    UUID donacionId = crearDonacion(donanteId, "Fideos");
    assertEquals(200, request("GET", "/api/v1/donaciones", null).statusCode());
    HttpResponse<String> donacionActualizada =
        request(
            "PUT",
            "/api/v1/donaciones/" + donacionId,
            """
            {"bienes":[{"descripcion":"Fideos largos","cantidad":12,"unidad":"KILOGRAMOS","estadoDeUso":"NO_APLICA","subcategoria":"Fideos","categoria":"ALIMENTO"}]}
            """);
    assertEquals(200, donacionActualizada.statusCode());

    assertEquals(204, request("DELETE", "/api/v1/donaciones/" + donacionId, null).statusCode());
    assertEquals(204, request("DELETE", "/api/v1/necesidades/" + necesidadId, null).statusCode());
    assertEquals(
        204,
        request("DELETE", "/api/v1/beneficiarias/" + entidadId, null)
            .statusCode());
    assertEquals(204, request("DELETE", "/api/v1/donantes/" + donanteId, null).statusCode());

    HttpResponse<String> inexistente =
        request("GET", "/api/v1/donaciones/" + UUID.randomUUID(), null);
    assertEquals(404, inexistente.statusCode());
    assertEquals("NOT_FOUND", json(inexistente).get("code").asText());

    HttpResponse<String> invalido = request("POST", "/api/v1/donantes", "{}");
    assertEquals(400, invalido.statusCode());
    assertEquals("VALIDATION_ERROR", json(invalido).get("code").asText());
    assertFalse(json(invalido).get("details").isEmpty());
  }

  @Test
  void transicionesRecepcionYFotosQuedanAuditadas() throws Exception {
    UUID donanteId = crearDonante();
    UUID entidadId = crearEntidad("Hogar Auditada");
    crearNecesidad(entidadId, "Leche");
    UUID donacionId = crearDonacion(donanteId, "Leche");

    assertEquals(
        200,
        request(
                "PATCH",
                "/api/v1/donaciones/" + donacionId + "/estado",
                "{\"estado\":\"ASIGNACION_REALIZADA\",\"entidadBeneficiariaId\":\"" + entidadId + "\"}")
            .statusCode());
    assertEquals(
        200,
        request(
                "PATCH",
                "/api/v1/donaciones/" + donacionId + "/estado",
                "{\"estado\":\"LISTA_PARA_ENTREGAR\"}")
            .statusCode());
    assertEquals(
        200,
        request(
                "PATCH",
                "/api/v1/donaciones/" + donacionId + "/estado",
                "{\"estado\":\"EN_TRASLADO\"}")
            .statusCode());

    HttpResponse<String> noRecibida =
        request(
            "PATCH",
            "/api/v1/donaciones/" + donacionId + "/recepcion",
            "{\"recibido\":false,\"justificacion\":\"No había responsables\"}");
    assertEquals(200, noRecibida.statusCode());
    assertEquals("ENTREGA_FALLIDA", json(noRecibida).get("estado").asText());

    HttpResponse<String> historial =
        request("GET", "/api/v1/donaciones/" + donacionId + "/historial-estados", null);
    assertEquals(200, historial.statusCode());
    JsonNode eventos = json(historial);
    assertEquals(4, eventos.size());
    assertEquals(1, eventos.get(0).get("secuencia").asInt());
    assertEquals(4, eventos.get(3).get("secuencia").asInt());
    assertEquals("No había responsables", eventos.get(3).get("justificacion").asText());

    HttpResponse<String> foto =
        request(
            "POST",
            "/api/v1/donaciones/" + donacionId + "/fotos",
            "{\"url\":\"https://cdn.example.org/evidencia.jpg\"}");
    assertEquals(201, foto.statusCode());
    assertEquals("https://cdn.example.org/evidencia.jpg", json(foto).get("url").asText());

    UUID donacionRecibidaId = crearDonacion(donanteId, "Leche");
    llevarATraslado(donacionRecibidaId, entidadId);
    HttpResponse<String> recibida =
        request(
            "PATCH",
            "/api/v1/donaciones/" + donacionRecibidaId + "/recepcion",
            "{\"recibido\":true}");
    assertEquals(200, recibida.statusCode());
    assertEquals("ENTREGADA", json(recibida).get("estado").asText());

    HttpResponse<String> entidad =
        request("GET", "/api/v1/beneficiarias/" + entidadId, null);
    assertEquals(1, json(entidad).get("recepciones").asInt());
  }

  @Test
  void matchmakingSeEjecutaConsultaYSeleccionaADemanda() throws Exception {
    UUID donanteId = crearDonante();
    UUID entidadId = crearEntidad("Comedor Match");
    crearNecesidad(entidadId, "Arroz");
    UUID donacionId = crearDonacion(donanteId, "Arroz");

    HttpResponse<String> ejecucion =
        request(
            "POST", "/api/v1/donaciones/" + donacionId + "/propuesta-asignacion", null);
    assertEquals(201, ejecucion.statusCode());
    assertEquals(donacionId.toString(), json(ejecucion).get("donacionId").asText());
    assertFalse(json(ejecucion).get("rankings").isEmpty());

    HttpResponse<String> ranking =
        request("GET", "/api/v1/donaciones/" + donacionId + "/propuesta-asignacion", null);
    assertEquals(200, ranking.statusCode());
    assertTrue(ranking.body().contains(entidadId.toString()));

    HttpResponse<String> seleccion =
        request(
            "PUT",
            "/api/v1/donaciones/" + donacionId + "/asignacion",
            "{\"entidadBeneficiariaId\":\"" + entidadId + "\"}");
    assertEquals(200, seleccion.statusCode());
    assertEquals("ASIGNACION_REALIZADA", json(seleccion).get("estado").asText());
    assertEquals(entidadId.toString(), json(seleccion).get("entidadBeneficiariaId").asText());
  }

  private UUID crearDonante() throws Exception {
    HttpResponse<String> response =
        request(
            "POST",
            "/api/v1/donantes",
            donanteJson("Sofía", "Pérez", "sofia@example.org"));
    assertEquals(201, response.statusCode(), response.body());
    return UUID.fromString(json(response).get("id").asText());
  }

  private UUID crearEntidad(String razonSocial) throws Exception {
    HttpResponse<String> response =
        request(
            "POST",
            "/api/v1/beneficiarias",
            "{\"razonSocial\":\""
                + razonSocial
                + "\",\"direccion\":\"Calle 123\",\"telefono\":\"1111111111\",\"correos\":[\"entidad@example.org\"]}");
    assertEquals(201, response.statusCode(), response.body());
    return UUID.fromString(json(response).get("id").asText());
  }

  private UUID crearNecesidad(UUID entidadId, String subcategoria) throws Exception {
    HttpResponse<String> response =
        request("POST", "/api/v1/necesidades", necesidadJson(entidadId, subcategoria, 10));
    assertEquals(201, response.statusCode(), response.body());
    return UUID.fromString(json(response).get("id").asText());
  }

  private UUID crearDonacion(UUID donanteId, String subcategoria) throws Exception {
    HttpResponse<String> response =
        request(
            "POST",
            "/api/v1/donaciones",
            "{\"donanteId\":\""
                + donanteId
                + "\",\"bienes\":[{\"descripcion\":\"Producto\",\"cantidad\":10,\"unidad\":\"KILOGRAMOS\",\"estadoDeUso\":\"NO_APLICA\",\"subcategoria\":\""
                + subcategoria
                + "\",\"categoria\":\"ALIMENTO\"}]}");
    assertEquals(201, response.statusCode(), response.body());
    return UUID.fromString(json(response).get("id").asText());
  }

  private void llevarATraslado(UUID donacionId, UUID entidadId) throws Exception {
    assertEquals(
        200,
        request(
                "PATCH",
                "/api/v1/donaciones/" + donacionId + "/estado",
                "{\"estado\":\"ASIGNACION_REALIZADA\",\"entidadBeneficiariaId\":\"" + entidadId + "\"}")
            .statusCode());
    assertEquals(
        200,
        request(
                "PATCH",
                "/api/v1/donaciones/" + donacionId + "/estado",
                "{\"estado\":\"LISTA_PARA_ENTREGAR\"}")
            .statusCode());
    assertEquals(
        200,
        request(
                "PATCH",
                "/api/v1/donaciones/" + donacionId + "/estado",
                "{\"estado\":\"EN_TRASLADO\"}")
            .statusCode());
  }

  private String donanteJson(String nombre, String apellido, String email) {
    return "{\"tipoPersona\":\"HUMANA\",\"numeroDocumento\":\"30111222\",\"tipoDocumento\":\"DNI\",\"mediosDeContacto\":[{\"tipo\":\"EMAIL\",\"contacto\":\""
        + email
        + "\"}],\"contactoPredeterminado\":0,\"nombre\":\""
        + nombre
        + "\",\"apellido\":\""
        + apellido
        + "\",\"edad\":30,\"genero\":\"FEMENINO\",\"direccion\":\"Calle 123\"}";
  }

  private String necesidadJson(UUID entidadId, String subcategoria, int cantidad) {
    return "{\"entidadBeneficiariaId\":\""
        + entidadId
        + "\",\"tipo\":\"EXTRAORDINARIA\",\"descripcion\":\"Alimentos\",\"cantidadObjetivo\":"
        + cantidad
        + ",\"unidad\":\"KILOGRAMOS\",\"subcategoria\":\""
        + subcategoria
        + "\",\"categoria\":\"ALIMENTO\"}";
  }

  private HttpResponse<String> request(String method, String path, String body)
      throws IOException, InterruptedException {
    HttpRequest.BodyPublisher publisher =
        body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body);
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(baseUrl + path))
            .header("Content-Type", "application/json")
            .method(method, publisher)
            .build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private JsonNode json(HttpResponse<String> response) throws IOException {
    return mapper.readTree(response.body());
  }
}
