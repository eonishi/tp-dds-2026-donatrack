package ar.edu.utn.donatrack.utilidades;

import ar.edu.utn.donatrack.donantes.MedioDeContacto;

public class NotificadorSimulado implements Notificador {
  @Override
  public void enviarNotificacion(MedioDeContacto destinatario, String mensaje) {
    if (destinatario != null) {
      NotificacionSimulada notif =
          new NotificacionSimulada(
              destinatario.getContacto(),
              mensaje,
              destinatario.getClass().getSimpleName().toUpperCase(),
              false);
      notif.completar();
    }
  }
}
