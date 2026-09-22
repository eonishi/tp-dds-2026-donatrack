package ar.edu.utn.donatrack.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.persistencia.JpaRepository;
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

class DonacionesApiHttpTest implements JpaRepository {
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

  @AfterEach
  void limpiarPersistencia() {
    // Las solicitudes HTTP confirman sus propias transacciones, a diferencia de las pruebas
    // transaccionales que revierten: se borran las donaciones confirmadas, los donantes y las
    // entidades beneficiarias creadas por la API para que no contaminen los conteos ni el chequeo
    // de documentos o razones sociales duplicadas de las demás pruebas. Las donaciones se borran
    // primero porque referencian a las entidades y a los donantes.
    inTransaction(
        () -> {
          createQuery("select d from Donacion d", Donacion.class)
              .getResultList()
              .forEach(this::remove);
          createQuery("select d from Donante d", Donante.class)
              .getResultList()
              .forEach(this::remove);
          createQuery("select e from EntidadBeneficiaria e", EntidadBeneficiaria.class)
              .getResultList()
              .forEach(this::remove);
        });
  }

  @Test
  void crudsCompletosRespetanCodigosHttpYErrores() throws Exception {
    UUID donanteId = crearDonante();
    assertEquals(200, request("GET", "/api/v1/donantes", null).statusCode());
    assertEquals(404, request("GET", "/donantes", null).statusCode());
    assertEquals(200, request("GET", "/api/v1/donantes/" + donanteId, null).statusCode());

    HttpResponse<String> donanteActualizado =
        request(
            "PUT",
            "/api/v1/donantes/" + donanteId,
            donanteJson("Ana", "Gómez", "ana.gomez@example.org"));
    assertEquals(200, donanteActualizado.statusCode());
    assertEquals("Ana", json(donanteActualizado).get("nombre").asText());

    long entidadId = crearEntidad("Comedor Central");
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
    assertEquals(
        200, request("GET", "/api/v1/entidades/" + entidadId + "/necesidades", null).statusCode());
    HttpResponse<String> necesidadActualizada =
        request(
            "PUT",
            "/api/v1/entidades/" + entidadId + "/necesidades/" + necesidadId,
            necesidadJson("Fideos", 20));
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
    assertEquals(
        204,
        request("DELETE", "/api/v1/entidades/" + entidadId + "/necesidades/" + necesidadId, null)
            .statusCode());
    assertEquals(204, request("DELETE", "/api/v1/beneficiarias/" + entidadId, null).statusCode());
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
  void necesidadesSoloSeResuelvenDentroDeSuEntidad() throws Exception {
    long entidadId = crearEntidad("Comedor Propietario");
    long otraEntidadId = crearEntidad("Comedor Ajeno");
    UUID necesidadId = crearNecesidad(entidadId, "Harina");

    String rutaAjena = "/api/v1/entidades/" + otraEntidadId + "/necesidades/" + necesidadId;
    assertEquals(404, request("GET", rutaAjena, null).statusCode());
    assertEquals(404, request("PUT", rutaAjena, necesidadJson("Harina", 20)).statusCode());
    assertEquals(404, request("DELETE", rutaAjena, null).statusCode());
    assertEquals(
        404,
        request("GET", "/api/v1/entidades/" + Long.MAX_VALUE + "/necesidades", null).statusCode());
    assertEquals(
        404,
        request(
                "POST",
                "/api/v1/entidades/" + Long.MAX_VALUE + "/necesidades",
                necesidadJson("Harina", 10))
            .statusCode());
    assertEquals(404, request("GET", "/api/v1/necesidades/" + necesidadId, null).statusCode());

    assertEquals(
        200,
        request("GET", "/api/v1/entidades/" + entidadId + "/necesidades/" + necesidadId, null)
            .statusCode());
  }

  @Test
  void transicionesDeEntregaYFotosQuedanAuditadas() throws Exception {
    UUID donanteId = crearDonante();
    long entidadId = crearEntidad("Hogar Auditada");
    crearNecesidad(entidadId, "Leche");
    UUID donacionId = crearDonacion(donanteId, "Leche");

    asignarDesdePropuesta(donacionId, entidadId);
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
            "/api/v1/donaciones/" + donacionId + "/estado",
            "{\"estado\":\"ENTREGA_FALLIDA\",\"justificacion\":\"No había responsables\"}");
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
            "/api/v1/donaciones/" + donacionRecibidaId + "/estado",
            "{\"estado\":\"ENTREGADA\"}");
    assertEquals(200, recibida.statusCode());
    assertEquals("ENTREGADA", json(recibida).get("estado").asText());
  }

  @Test
  void matchmakingSeEjecutaConsultaYSeleccionaADemanda() throws Exception {
    UUID donanteId = crearDonante();
    long entidadId = crearEntidad("Comedor Match");
    crearNecesidad(entidadId, "Arroz");
    UUID donacionId = crearDonacion(donanteId, "Arroz");
    UUID otraDonacionId = crearDonacion(donanteId, "Arroz");

    HttpResponse<String> ejecucion =
        request("POST", "/api/v1/donaciones/" + donacionId + "/propuesta-asignacion", null);
    assertEquals(201, ejecucion.statusCode());
    assertEquals(donacionId.toString(), json(ejecucion).get("donacionId").asText());
    assertFalse(json(ejecucion).get("rankings").isEmpty());
    assertEquals(
        201,
        request("POST", "/api/v1/donaciones/" + otraDonacionId + "/propuesta-asignacion", null)
            .statusCode());

    HttpResponse<String> propuestas = request("GET", "/api/v1/propuestas-asignacion", null);
    assertEquals(200, propuestas.statusCode());
    assertEquals(2, json(propuestas).size());
    assertTrue(propuestas.body().contains(donacionId.toString()));
    assertTrue(propuestas.body().contains(otraDonacionId.toString()));

    HttpResponse<String> ranking =
        request("GET", "/api/v1/donaciones/" + donacionId + "/propuesta-asignacion", null);
    assertEquals(200, ranking.statusCode());
    assertTrue(ranking.body().contains(Long.toString(entidadId)));

    HttpResponse<String> seleccion =
        request(
            "PUT",
            "/api/v1/donaciones/" + donacionId + "/propuesta-asignacion",
            "{\"entidadBeneficiariaId\":" + entidadId + "}");
    assertEquals(200, seleccion.statusCode());
    assertEquals("ASIGNACION_REALIZADA", json(seleccion).get("estado").asText());
    assertEquals(entidadId, json(seleccion).get("entidadBeneficiariaId").asLong());
    HttpResponse<String> pendientes = request("GET", "/api/v1/propuestas-asignacion", null);
    assertEquals(200, pendientes.statusCode());
    assertEquals(1, json(pendientes).size());
    assertEquals(otraDonacionId.toString(), json(pendientes).get(0).get("donacionId").asText());
    assertEquals(
        404, request("PUT", "/api/v1/donaciones/" + donacionId + "/asignacion", null).statusCode());
    assertEquals(404, request("POST", "/api/v1/propuestas-asignacion", null).statusCode());
    assertEquals(404, request("PUT", "/api/v1/propuestas-asignacion", null).statusCode());
    assertEquals(404, request("DELETE", "/api/v1/propuestas-asignacion", null).statusCode());
  }

  @Test
  void seleccionDeEntidadFueraDelRankingDevuelveConflict() throws Exception {
    UUID donanteId = crearDonante();
    long entidadRankeadaId = crearEntidad("Comedor Match");
    long entidadNoRankeadaId = crearEntidad("Comedor Sin Match");
    crearNecesidad(entidadRankeadaId, "Arroz");
    UUID donacionId = crearDonacion(donanteId, "Arroz");

    assertEquals(
        201,
        request("POST", "/api/v1/donaciones/" + donacionId + "/propuesta-asignacion", null)
            .statusCode());

    HttpResponse<String> seleccion =
        request(
            "PUT",
            "/api/v1/donaciones/" + donacionId + "/propuesta-asignacion",
            "{\"entidadBeneficiariaId\":" + entidadNoRankeadaId + "}");

    assertEquals(409, seleccion.statusCode());
    assertEquals("CONFLICT", json(seleccion).get("code").asText());
    assertEquals(
        "La entidad no pertenece al ranking vigente.", json(seleccion).get("message").asText());
  }

  @Test
  void patchNoPermiteBypassearLaAsignacionAdministrativa() throws Exception {
    UUID donanteId = crearDonante();
    long entidadRankeadaId = crearEntidad("Comedor Match");
    crearNecesidad(entidadRankeadaId, "Arroz");
    UUID donacionId = crearDonacion(donanteId, "Arroz");

    HttpResponse<String> respuesta =
        request(
            "PATCH",
            "/api/v1/donaciones/" + donacionId + "/estado",
            "{\"estado\":\"ASIGNACION_REALIZADA\"}");

    assertEquals(400, respuesta.statusCode());
    assertEquals("VALIDATION_ERROR", json(respuesta).get("code").asText());
    assertEquals(
        "EN_DEPOSITO",
        json(request("GET", "/api/v1/donaciones/" + donacionId, null)).get("estado").asText());
  }

  @Test
  void cargaUnaDonacionLaSegmentaYDevuelveLasDonacionesCreadas() throws Exception {
    UUID donanteId = crearDonante();

    HttpResponse<String> response =
        request(
            "POST",
            "/api/v1/donaciones",
            """
            {"donanteId":"%s","bienes":[
              {"descripcion":"Silla nueva","cantidad":2,"unidad":"UNIDADES","estadoDeUso":"NUEVO","subcategoria":"Sillas","categoria":"MOBILIARIO"},
              {"descripcion":"Silla usada","cantidad":3,"unidad":"UNIDADES","estadoDeUso":"USADO","subcategoria":"Sillas","categoria":"MOBILIARIO"},
              {"descripcion":"Mesa nueva","cantidad":1,"unidad":"UNIDADES","estadoDeUso":"NUEVO","subcategoria":"Mesas","categoria":"MOBILIARIO"}
            ]}
            """
                .formatted(donanteId));

    assertEquals(201, response.statusCode(), response.body());
    JsonNode donaciones = json(response);
    assertEquals(3, donaciones.size());
    for (JsonNode donacion : donaciones) {
      assertEquals(donanteId.toString(), donacion.get("donanteId").asText());
      assertEquals(1, donacion.get("bienes").size());
    }
  }

  private UUID crearDonante() throws Exception {
    HttpResponse<String> response =
        request("POST", "/api/v1/donantes", donanteJson("Sofía", "Pérez", "sofia@example.org"));
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
                + "\",\"direccion\":\"Calle 123\",\"telefono\":\"1111111111\",\"correos\":[\"entidad@example.org\"]}");
    assertEquals(201, response.statusCode(), response.body());
    return Long.parseLong(json(response).get("id").asText());
  }

  private UUID crearNecesidad(long entidadId, String subcategoria) throws Exception {
    HttpResponse<String> response =
        request(
            "POST",
            "/api/v1/entidades/" + entidadId + "/necesidades",
            necesidadJson(subcategoria, 10));
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
    return UUID.fromString(json(response).get(0).get("id").asText());
  }

  private void llevarATraslado(UUID donacionId, long entidadId) throws Exception {
    asignarDesdePropuesta(donacionId, entidadId);
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

  private void asignarDesdePropuesta(UUID donacionId, long entidadId) throws Exception {
    assertEquals(
        201,
        request("POST", "/api/v1/donaciones/" + donacionId + "/propuesta-asignacion", null)
            .statusCode());
    assertEquals(
        200,
        request(
                "PUT",
                "/api/v1/donaciones/" + donacionId + "/propuesta-asignacion",
                "{\"entidadBeneficiariaId\":" + entidadId + "}")
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

  private String necesidadJson(String subcategoria, int cantidad) {
    return "{\"tipo\":\"EXTRAORDINARIA\",\"descripcion\":\"Alimentos\",\"cantidadObjetivo\":"
        + cantidad
        + ",\"unidad\":\"KILOGRAMOS\",\"subcategoria\":\""
        + subcategoria
        + "\",\"categoria\":\"ALIMENTO\"}";
  }

  private HttpResponse<String> request(String method, String path, String body)
      throws IOException, InterruptedException {
    HttpRequest.BodyPublisher publisher =
        body == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(body);
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
