package ar.edu.utn.donatrack.notificaciones;

import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.MedioDeContacto;
import ar.edu.utn.donatrack.donantes.Sms;
import ar.edu.utn.donatrack.donantes.Whatsapp;

public interface Notificador {
  void enviarNotificacion(MedioDeContacto destinatario, String mensaje);

  void enviar(CorreoElectronico destinatario, String mensaje);

  void enviar(Whatsapp destinatario, String mensaje);

  void enviar(Sms destinatario, String mensaje);
}
