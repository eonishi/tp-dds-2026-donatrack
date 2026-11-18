package ar.edu.utn.donatrack.logistica.planificacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.donatrack.logistica.Camion;
import ar.edu.utn.donatrack.logistica.monitoreo.Coordenada;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Prueba el adaptador HTTP contra un servidor falso en proceso: verifica el cuerpo enviado, los
 * headers y el manejo de errores, sin invocar a ningún proveedor real.
 */
class PlanificadorRutasHttpTest {

  private static final String URL_CALLBACK =
      "http://localhost:9001/logistica/planificacion/callback";

  private HttpServer servidor;
  private final AtomicReference<String> metodo = new AtomicReference<>();
  private final AtomicReference<String> ruta = new AtomicReference<>();
  private final AtomicReference<String> contentType = new AtomicReference<>();
  private final AtomicReference<String> authorization = new AtomicReference<>();
  private final AtomicReference<String> cuerpo = new AtomicReference<>();

  private int estadoRespuesta = 200;
  private String cuerpoRespuesta = "";
  private String urlBase;

  @BeforeEach
  void levantarServidorFalso() throws Exception {
    servidor = HttpServer.create(new InetSocketAddress(0), 0);
    servidor.createContext(
        "/lotes",
        intercambio -> {
          metodo.set(intercambio.getRequestMethod());
          ruta.set(intercambio.getRequestURI().getPath());
          contentType.set(intercambio.getRequestHeaders().getFirst("Content-Type"));
          authorization.set(intercambio.getRequestHeaders().getFirst("Authorization"));
          cuerpo.set(
              new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

          byte[] respuesta = cuerpoRespuesta.getBytes(StandardCharsets.UTF_8);
          intercambio.getResponseHeaders().set("Content-Type", "application/json");
          intercambio.sendResponseHeaders(estadoRespuesta, respuesta.length);
          intercambio.getResponseBody().write(respuesta);
          intercambio.close();
        });
    servidor.start();
    urlBase = "http://localhost:" + servidor.getAddress().getPort();
  }

  @AfterEach
  void bajarServidorFalso() {
    servidor.stop(0);
  }

  private PlanificadorRutasHttp crearAdaptador() {
    return new PlanificadorRutasHttp(
        urlBase, "clave-del-planificador", HttpClient.newHttpClient(), new ObjectMapper());
  }

  private static Camion crearCamion(String patente) {
    return new Camion(patente, 45.0, 2.5, 5000.0);
  }

  private static DonacionPlanificable crearDonacion(UUID id, long entidadId) {
    return new DonacionPlanificable(
        id, entidadId, "Av. Mitre 750", new Coordenada(-34.662, -58.365), 1.5, 30.0);
  }

  @Test
  void enviarLote_EnviaElLotePorPostYDevuelveElLoteIdDelProveedor() throws Exception {
    UUID loteId = UUID.randomUUID();
    cuerpoRespuesta = "{\"loteId\":\"" + loteId + "\"}";
    PlanificadorRutasHttp planificador = crearAdaptador();
    Camion camion = crearCamion("ABC123");
    UUID donacionId = UUID.randomUUID();
    DonacionPlanificable donacion = crearDonacion(donacionId, 42L);

    UUID devuelto =
        planificador.enviarLote(
            new LoteDonaciones(List.of(donacion), List.of(camion), URL_CALLBACK));

    assertEquals(loteId, devuelto);
    assertEquals("POST", metodo.get());
    assertEquals("/lotes", ruta.get());
    assertEquals("application/json", contentType.get());
    assertEquals("clave-del-planificador", authorization.get());

    JsonNode cuerpo = new ObjectMapper().readTree(this.cuerpo.get());
    assertEquals(1, cuerpo.get("donaciones").size());
    JsonNode donacionJson = cuerpo.get("donaciones").get(0);
    assertEquals(donacionId.toString(), donacionJson.get("idDonacion").asText());
    assertEquals(42L, donacionJson.get("idEntidadBeneficiaria").asLong());
    assertEquals("Av. Mitre 750", donacionJson.get("direccionDestino").asText());
    assertEquals(-34.662, donacionJson.get("coordenadas").get("lat").asDouble(), 0.001);
    assertEquals(-58.365, donacionJson.get("coordenadas").get("lng").asDouble(), 0.001);
    assertEquals(1.5, donacionJson.get("volumenM3").asDouble(), 0.001);
    assertEquals(30.0, donacionJson.get("pesoKg").asDouble(), 0.001);

    assertEquals(1, cuerpo.get("camiones").size());
    JsonNode camionJson = cuerpo.get("camiones").get(0);
    assertEquals("ABC123", camionJson.get("patente").asText());
    assertEquals(45.0, camionJson.get("volumenM3").asDouble(), 0.001);
    assertEquals(2.5, camionJson.get("alturaM").asDouble(), 0.001);
    assertEquals(5000.0, camionJson.get("capacidadCargaKg").asDouble(), 0.001);
    assertEquals(URL_CALLBACK, cuerpo.get("urlCallback").asText());
  }

  @Test
  void enviarLote_SerializaTodasLasDonacionesDelLote() throws Exception {
    UUID loteId = UUID.randomUUID();
    cuerpoRespuesta = "{\"loteId\":\"" + loteId + "\"}";
    PlanificadorRutasHttp planificador = crearAdaptador();
    Camion camion = crearCamion("ABC123");
    UUID primera = UUID.randomUUID();
    UUID segunda = UUID.randomUUID();

    planificador.enviarLote(
        new LoteDonaciones(
            List.of(crearDonacion(primera, 1L), crearDonacion(segunda, 2L)),
            List.of(camion),
            URL_CALLBACK));

    JsonNode cuerpo = new ObjectMapper().readTree(this.cuerpo.get());
    assertEquals(2, cuerpo.get("donaciones").size());
    assertEquals(primera.toString(), cuerpo.get("donaciones").get(0).get("idDonacion").asText());
    assertEquals(segunda.toString(), cuerpo.get("donaciones").get(1).get("idDonacion").asText());
  }

  @Test
  void enviarLote_CuandoElProveedorRespondeError_LanzaExcepcionConElDetalle() {
    estadoRespuesta = 500;
    cuerpoRespuesta = "{\"detail\":\"flota sin camiones disponibles\"}";
    PlanificadorRutasHttp planificador = crearAdaptador();
    LoteDonaciones lote =
        new LoteDonaciones(
            List.of(crearDonacion(UUID.randomUUID(), 42L)),
            List.of(crearCamion("ABC123")),
            URL_CALLBACK);

    RuntimeException ex = assertThrows(RuntimeException.class, () -> planificador.enviarLote(lote));

    assertTrue(
        ex.getMessage().contains("flota sin camiones disponibles"),
        "el mensaje debe incluir el detalle devuelto por el proveedor: " + ex.getMessage());
  }

  @Test
  void enviarLote_CuandoElErrorNoEsJson_UsaElCuerpoCrudoEnElMensaje() {
    estadoRespuesta = 400;
    cuerpoRespuesta = "payload inválido";
    PlanificadorRutasHttp planificador = crearAdaptador();
    LoteDonaciones lote =
        new LoteDonaciones(
            List.of(crearDonacion(UUID.randomUUID(), 42L)),
            List.of(crearCamion("ABC123")),
            URL_CALLBACK);

    RuntimeException ex = assertThrows(RuntimeException.class, () -> planificador.enviarLote(lote));

    assertTrue(ex.getMessage().contains("payload inválido"));
  }

  @Test
  void enviarLote_CuandoElProveedorRespondeSinLoteId_LanzaExcepcion() {
    estadoRespuesta = 200;
    cuerpoRespuesta = "{}";
    PlanificadorRutasHttp planificador = crearAdaptador();
    LoteDonaciones lote =
        new LoteDonaciones(
            List.of(crearDonacion(UUID.randomUUID(), 42L)),
            List.of(crearCamion("ABC123")),
            URL_CALLBACK);

    RuntimeException ex = assertThrows(RuntimeException.class, () -> planificador.enviarLote(lote));

    assertTrue(ex.getMessage().contains("falta loteId"));
  }
}
