package ar.edu.utn.donatrack.ServiciosExternosYUtilidades;

import ar.edu.utn.donatrack.DonantesYContacto.MedioDeContacto;
import java.util.ArrayList;
import java.util.List;

public class NotificadorSimulado implements Notificador {
  private final List<NotificacionSimulada> enviadas = new ArrayList<>();

  @Override
  public void enviarNotificacion(MedioDeContacto destinatario, String mensaje) {
    if (destinatario == null) return;
    NotificacionSimulada notif = new NotificacionSimulada(
        destinatario.getContacto(),
        mensaje,
        destinatario.getClass().getSimpleName().toUpperCase(),
        false
    );
    // Simular completado exitoso del envío
    notif.completar();
    enviadas.add(notif);
  }

  public List<NotificacionSimulada> getEnviadas() {
    return enviadas;
  }
}
