package ar.edu.utn.donatrack.notificaciones;

import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.MedioDeContacto;
import ar.edu.utn.donatrack.donantes.Sms;
import ar.edu.utn.donatrack.donantes.Whatsapp;

public interface Notificador {
  ResultadoEnvio enviarNotificacion(MedioDeContacto destinatario, String mensaje);

  ResultadoEnvio enviar(CorreoElectronico destinatario, String mensaje);

  ResultadoEnvio enviar(Whatsapp destinatario, String mensaje);

  ResultadoEnvio enviar(Sms destinatario, String mensaje);
}
