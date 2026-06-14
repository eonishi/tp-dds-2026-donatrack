package ar.edu.utn.donatrack.utilidades;

import ar.edu.utn.donatrack.donantes.MedioDeContacto;

public interface Notificador {
  void enviarNotificacion(MedioDeContacto destinatario, String mensaje);
}
