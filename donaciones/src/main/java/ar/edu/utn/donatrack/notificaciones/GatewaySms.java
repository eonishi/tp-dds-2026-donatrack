package ar.edu.utn.donatrack.notificaciones;

public interface GatewaySms {
  ResultadoEnvio enviar(String destinatario, String mensaje);
}
