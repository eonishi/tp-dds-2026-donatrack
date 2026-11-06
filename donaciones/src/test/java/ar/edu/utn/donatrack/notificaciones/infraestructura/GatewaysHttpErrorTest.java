package ar.edu.utn.donatrack.notificaciones.infraestructura;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import ar.edu.utn.donatrack.notificaciones.ResultadoEnvio;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

class GatewaysHttpErrorTest {
  @Test
  void resendConservaStatusYBodySanitizadoSinExponerApiKey() throws Exception {
    HttpClient client = mock(HttpClient.class);
    HttpResponse<String> response = mock(HttpResponse.class);
    doReturn(422).when(response).statusCode();
    doReturn("detalle inválido\napi-key-super-secreta").when(response).body();
    doReturn(response).when(client).send(any(), any());

    ResultadoEnvio resultado =
        new GatewayCorreoResend(client, "api-key-super-secreta", "noreply@example.org")
            .enviar("persona@example.org", "Asunto", "Mensaje");

    assertFalse(resultado.exitoso());
    assertTrue(resultado.detalle().contains("HTTP 422"));
    assertTrue(resultado.detalle().contains("detalle inválido"));
    assertFalse(resultado.detalle().contains("api-key-super-secreta"));
    assertFalse(resultado.detalle().contains("\n"));
  }

  @Test
  void twilioConservaStatusYTruncaElBodySinExponerToken() throws Exception {
    HttpClient client = mock(HttpClient.class);
    HttpResponse<String> response = mock(HttpResponse.class);
    doReturn(400).when(response).statusCode();
    doReturn("auth-token-secreto " + "x".repeat(600)).when(response).body();
    doReturn(response).when(client).send(any(), any());

    ResultadoEnvio resultado =
        new GatewayTwilio(client, "AC123", "auth-token-secreto", "+15005550006")
            .enviar("+541112345678", "Mensaje");

    assertFalse(resultado.exitoso());
    assertTrue(resultado.detalle().contains("HTTP 400"));
    assertFalse(resultado.detalle().contains("auth-token-secreto"));
    assertTrue(resultado.detalle().length() < 400);
  }

  @Test
  void whatsappConservaStatusYBodySanitizadoSinExponerCredenciales() throws Exception {
    HttpClient client = mock(HttpClient.class);
    HttpResponse<String> response = mock(HttpResponse.class);
    doReturn(429).when(response).statusCode();
    doReturn("cuota agotada\tAC-secreto\nauth-token-secreto").when(response).body();
    doReturn(response).when(client).send(any(), any());

    ResultadoEnvio resultado =
        new GatewayWhatsappTwilio(
                client, "AC-secreto", "auth-token-secreto", "whatsapp:+15005550006")
            .enviar("+541112345678", "Mensaje");

    assertFalse(resultado.exitoso());
    assertTrue(resultado.detalle().contains("HTTP 429"));
    assertTrue(resultado.detalle().contains("cuota agotada"));
    assertFalse(resultado.detalle().contains("AC-secreto"));
    assertFalse(resultado.detalle().contains("auth-token-secreto"));
    assertFalse(resultado.detalle().contains("\n"));
    assertFalse(resultado.detalle().contains("\t"));
  }
}
