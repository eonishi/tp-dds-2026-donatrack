package ar.edu.utn.donatrack.notificaciones;

import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.MedioDeContacto;
import ar.edu.utn.donatrack.donantes.Sms;
import ar.edu.utn.donatrack.donantes.Whatsapp;

public class NotificadorSimulado implements Notificador {
  @Override
  public ResultadoEnvio enviarNotificacion(MedioDeContacto destinatario, String mensaje) {
    if (destinatario != null) {
      return destinatario.aceptar(this, mensaje);
    }
    return ResultadoEnvio.fallido("El destinatario es obligatorio.");
  }

  @Override
  public ResultadoEnvio enviar(CorreoElectronico destinatario, String mensaje) {
    System.out.println(
        "[SIMULADO][EMAIL] Para: " + destinatario.getContacto() + " | Msg: " + mensaje);
    return ResultadoEnvio.aceptado();
  }

  @Override
  public ResultadoEnvio enviar(Whatsapp destinatario, String mensaje) {
    System.out.println(
        "[SIMULADO][WHATSAPP] Para: " + destinatario.getContacto() + " | Msg: " + mensaje);
    return ResultadoEnvio.aceptado();
  }

  @Override
  public ResultadoEnvio enviar(Sms destinatario, String mensaje) {
    System.out.println(
        "[SIMULADO][SMS] Para: " + destinatario.getContacto() + " | Msg: " + mensaje);
    return ResultadoEnvio.aceptado();
  }
}
