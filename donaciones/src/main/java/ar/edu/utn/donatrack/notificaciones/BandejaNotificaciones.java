package ar.edu.utn.donatrack.notificaciones;

import java.util.List;

/** Guarda en la bandeja web los avisos que se enviaron por correo, SMS o WhatsApp. */
public interface BandejaNotificaciones {
  BandejaNotificaciones NINGUNA = notificaciones -> {};

  void registrar(List<Notificacion> notificaciones);
}
