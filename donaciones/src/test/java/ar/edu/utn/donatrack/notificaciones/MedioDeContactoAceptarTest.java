package ar.edu.utn.donatrack.notificaciones;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.MedioDeContacto;
import ar.edu.utn.donatrack.donantes.Sms;
import ar.edu.utn.donatrack.donantes.Whatsapp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies that {@link MedioDeContacto#aceptar(Notificador, String)} dispatches to the correct
 * {@code Notificador.enviar()} overload based on the concrete subclass type.
 */
@ExtendWith(MockitoExtension.class)
class MedioDeContactoAceptarTest {

  @Mock Notificador notificador;

  @Test
  void correoElectronico_aceptar_routerAlOverloadDeCorreo() {
    CorreoElectronico correo = new CorreoElectronico("test@example.com");

    correo.aceptar(notificador, "Mensaje");

    verify(notificador).enviar(any(CorreoElectronico.class), eq("Mensaje"));
  }

  @Test
  void whatsapp_aceptar_routerAlOverloadDeWhatsapp() {
    Whatsapp wsp = new Whatsapp("+541112345678");

    wsp.aceptar(notificador, "Mensaje");

    verify(notificador).enviar(any(Whatsapp.class), eq("Mensaje"));
  }

  @Test
  void sms_aceptar_routerAlOverloadDeSms() {
    Sms sms = new Sms("+541112345679");

    sms.aceptar(notificador, "Mensaje");

    verify(notificador).enviar(any(Sms.class), eq("Mensaje"));
  }
}
