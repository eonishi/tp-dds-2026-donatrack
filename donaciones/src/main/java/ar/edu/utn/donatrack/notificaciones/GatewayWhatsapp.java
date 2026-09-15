package ar.edu.utn.donatrack.notificaciones;

public interface GatewayWhatsapp {
  ResultadoEnvio enviar(String destinatario, String mensaje);
}
