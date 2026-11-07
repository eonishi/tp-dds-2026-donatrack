package ar.edu.utn.donatrack.notificaciones;

public interface GatewayCorreo {
  ResultadoEnvio enviar(String destinatario, String asunto, String mensaje);
}
