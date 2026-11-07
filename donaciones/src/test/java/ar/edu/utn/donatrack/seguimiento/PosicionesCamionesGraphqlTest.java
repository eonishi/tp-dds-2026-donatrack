package ar.edu.utn.donatrack.seguimiento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import java.net.http.HttpClient;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** El cliente de donaciones contra un logística simulado que responde como el contrato GraphQL. */
class PosicionesCamionesGraphqlTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private final AtomicReference<String> autorizacionRecibida = new AtomicReference<>();
  private Javalin logistica;
  private PosicionesCamionesGraphql cliente;

  @BeforeEach
  void start() {
    logistica =
        Javalin.create()
            .post(
                "/logistica/graphql",
                ctx -> {
                  autorizacionRecibida.set(ctx.header("Authorization"));
                  JsonNode pedido = mapper.readTree(ctx.body());
                  String patente = pedido.path("variables").path("patente").asText();
                  ctx.contentType("application/json").result(respuestaPara(patente));
                })
            .start(0);
    cliente =
        new PosicionesCamionesGraphql(
            "http://localhost:" + logistica.port(), "clave", HttpClient.newHttpClient());
  }

  @AfterEach
  void stop() {
    logistica.stop();
  }

  private static String respuestaPara(String patente) {
    return switch (patente) {
      case "AB123CD" ->
          "{\"data\":{\"posicion\":{\"patente\":\"AB123CD\",\"latitud\":-34.6,"
              + "\"longitud\":-58.4,\"actualizadaEn\":\"2026-09-11T11:59\"}}}";
      case "SINLECTURAS" -> "{\"data\":{\"posicion\":null}}";
      case "NOEXISTE" ->
          "{\"data\":{\"posicion\":null},\"errors\":[{\"message\":\"Camión no encontrado\","
              + "\"extensions\":{\"codigo\":\"NO_ENCONTRADO\"}}]}";
      default ->
          "{\"data\":null,\"errors\":[{\"message\":\"Error interno\","
              + "\"extensions\":{\"codigo\":\"ERROR_INTERNO\"}}]}";
    };
  }

  @Test
  void leeLaPosicionYMandaLaClaveInterna() {
    PosicionCamion posicion = cliente.ultimaPosicion("AB123CD").orElseThrow();

    assertEquals(-34.6, posicion.latitud());
    assertEquals(-58.4, posicion.longitud());
    assertEquals("2026-09-11T11:59", posicion.actualizadaEn());
    assertEquals("clave", autorizacionRecibida.get());
  }

  @Test
  void unCamionSinLecturasOQueLogisticaNoConoceNoTienePosicion() {
    assertTrue(cliente.ultimaPosicion("SINLECTURAS").isEmpty());
    assertTrue(cliente.ultimaPosicion("NOEXISTE").isEmpty());
  }

  @Test
  void unErrorDeLogisticaSeInformaComoNoDisponible() {
    assertThrows(LogisticaNoDisponibleException.class, () -> cliente.ultimaPosicion("ROTO"));
  }
}
