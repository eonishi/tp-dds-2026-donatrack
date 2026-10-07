package ar.edu.utn.donatrack.notificaciones;

import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.MedioDeContacto;
import ar.edu.utn.donatrack.donantes.Sms;
import ar.edu.utn.donatrack.donantes.Whatsapp;
import java.util.Objects;

public class NotificadorPorCanal implements Notificador {
  private static final String ASUNTO_POR_DEFECTO = "Novedad de DonaTrack";

  private final GatewayCorreo correo;
  private final GatewaySms sms;
  private final GatewayWhatsapp whatsapp;

  public NotificadorPorCanal(GatewayCorreo correo, GatewaySms sms, GatewayWhatsapp whatsapp) {
    this.correo = Objects.requireNonNull(correo);
    this.sms = Objects.requireNonNull(sms);
    this.whatsapp = Objects.requireNonNull(whatsapp);
  }

  @Override
  public ResultadoEnvio enviarNotificacion(MedioDeContacto destinatario, String mensaje) {
    if (destinatario == null) {
      return ResultadoEnvio.fallido("El destinatario es obligatorio.");
    }
    return destinatario.aceptar(this, mensaje);
  }

  @Override
  public ResultadoEnvio enviar(CorreoElectronico destinatario, String mensaje) {
    return correo.enviar(destinatario.getContacto(), ASUNTO_POR_DEFECTO, mensaje);
  }

  @Override
  public ResultadoEnvio enviar(Whatsapp destinatario, String mensaje) {
    return whatsapp.enviar(destinatario.getContacto(), mensaje);
  }

  @Override
  public ResultadoEnvio enviar(Sms destinatario, String mensaje) {
    return sms.enviar(destinatario.getContacto(), mensaje);
  }
}
