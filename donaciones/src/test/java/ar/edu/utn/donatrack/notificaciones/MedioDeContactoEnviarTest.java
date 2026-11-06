package ar.edu.utn.donatrack.notificaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.Sms;
import ar.edu.utn.donatrack.donantes.Whatsapp;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies that each {@code MedioDeContacto} sends through its own channel gateway. */
class MedioDeContactoEnviarTest {
  private final List<String> invocaciones = new ArrayList<>();
  private final NotificadorPorCanal canales =
      new NotificadorPorCanal(
          (destinatario, asunto, mensaje) -> registrar("correo", destinatario, mensaje),
          (destinatario, mensaje) -> registrar("sms", destinatario, mensaje),
          (destinatario, mensaje) -> registrar("whatsapp", destinatario, mensaje));

  @Test
  void correoElectronico_enviaPorElGatewayDeCorreo() {
    Envio envio = new CorreoElectronico("test@example.com").enviar("Mensaje", canales);

    assertEquals(List.of("correo|test@example.com|Mensaje"), invocaciones);
    assertEquals(new Envio(Canal.CORREO, ResultadoEnvio.aceptado()), envio);
  }

  @Test
  void sms_enviaPorElGatewayDeSms() {
    Envio envio = new Sms("+541112345679").enviar("Mensaje", canales);

    assertEquals(List.of("sms|+541112345679|Mensaje"), invocaciones);
    assertEquals(new Envio(Canal.SMS, ResultadoEnvio.aceptado()), envio);
  }

  @Test
  void whatsapp_enviaPorElGatewayDeWhatsapp() {
    Envio envio = new Whatsapp("+541112345678").enviar("Mensaje", canales);

    assertEquals(List.of("whatsapp|+541112345678|Mensaje"), invocaciones);
    assertEquals(new Envio(Canal.WHATSAPP, ResultadoEnvio.aceptado()), envio);
  }

  private ResultadoEnvio registrar(String canal, String destinatario, String mensaje) {
    invocaciones.add(canal + "|" + destinatario + "|" + mensaje);
    return ResultadoEnvio.aceptado();
  }
}
