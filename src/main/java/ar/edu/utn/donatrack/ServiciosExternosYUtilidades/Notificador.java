package ar.edu.utn.donatrack.ServiciosExternosYUtilidades;

import ar.edu.utn.donatrack.DonantesYContacto.MedioDeContacto;

public interface Notificador {
  void enviarNotificacion(MedioDeContacto destinatario, String mensaje);
}