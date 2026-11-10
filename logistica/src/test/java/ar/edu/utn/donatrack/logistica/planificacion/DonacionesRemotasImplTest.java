package ar.edu.utn.donatrack.logistica.planificacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DonacionesRemotasImplTest {
  @Test
  void inicioTrasladoUsaApiV1YEnviaLaPatenteComoIdCamion() throws Exception {
    HttpClient client = mock(HttpClient.class);
    HttpResponse<String> response = mock(HttpResponse.class);
    doReturn(200).when(response).statusCode();
    doReturn("").when(response).body();
    ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
    doReturn(response).when(client).send(request.capture(), any());
    DonacionesRemotasImpl remotas =
        new DonacionesRemotasImpl("http://donaciones", "api-key", client, new ObjectMapper());

    remotas.notificarInicioTraslado(
        List.of(UUID.fromString("11111111-1111-1111-1111-111111111111")), "AB123CD");

    assertEquals("http://donaciones/api/v1/donaciones/estado", request.getValue().uri().toString());
    String body = body(request.getValue());
    assertTrue(body.contains("\"idCamion\":\"AB123CD\""));
    assertFalse(body.contains("https://mapa.donatrack.org"));
    assertFalse(request.getValue().uri().toString().contains("/api/donaciones/"));
  }

  private String body(HttpRequest request) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    request
        .bodyPublisher()
        .orElseThrow()
        .subscribe(
            new Flow.Subscriber<>() {
              @Override
              public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
              }

              @Override
              public void onNext(ByteBuffer item) {
                byte[] chunk = new byte[item.remaining()];
                item.get(chunk);
                bytes.writeBytes(chunk);
              }

              @Override
              public void onError(Throwable throwable) {
                throw new AssertionError(throwable);
              }

              @Override
              public void onComplete() {}
            });
    return bytes.toString(StandardCharsets.UTF_8);
  }
}
