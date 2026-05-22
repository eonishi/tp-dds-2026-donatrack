package ar.edu.utn.donatrack.ServiciosExternosYUtilidades;

import ar.edu.utn.donatrack.DonantesYContacto.MedioDeContacto;

public class NotificadorSimulado implements Notificador {
  @Override
  public void enviarNotificacion(MedioDeContacto destinatario, String mensaje) {
    if (destinatario == null) return;
    NotificacionSimulada notif = new NotificacionSimulada(
        destinatario.getContacto(),
        mensaje,
        destinatario.getClass().getSimpleName().toUpperCase(),
        false
    );
    notif.completar();
  }
}