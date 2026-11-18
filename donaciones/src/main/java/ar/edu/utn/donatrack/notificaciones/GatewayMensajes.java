package ar.edu.utn.donatrack.notificaciones;

/** Gateway de mensajes de texto a un número de teléfono (SMS o WhatsApp). */
public interface GatewayMensajes {
  ResultadoEnvio enviar(String destinatario, String mensaje);
}
