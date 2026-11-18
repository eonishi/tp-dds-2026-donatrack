package ar.edu.utn.donatrack.notificaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ar.edu.utn.donatrack.donantes.MedioDeContacto;
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
    MedioDeContacto.correo("test@example.com").enviar("Mensaje", canales);

    assertEquals(List.of("correo|test@example.com|Mensaje"), invocaciones);
  }

  @Test
  void sms_enviaPorElGatewayDeSms() {
    MedioDeContacto.sms("+541112345679").enviar("Mensaje", canales);

    assertEquals(List.of("sms|+541112345679|Mensaje"), invocaciones);
  }

  @Test
  void whatsapp_enviaPorElGatewayDeWhatsapp() {
    MedioDeContacto.whatsapp("+541112345678").enviar("Mensaje", canales);

    assertEquals(List.of("whatsapp|+541112345678|Mensaje"), invocaciones);
  }

  private ResultadoEnvio registrar(String canal, String destinatario, String mensaje) {
    invocaciones.add(canal + "|" + destinatario + "|" + mensaje);
    return ResultadoEnvio.aceptado();
  }
}
