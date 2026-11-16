package ar.edu.utn.donatrack.notificaciones;

import java.util.Objects;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gateways de envío por canal. Cada medio de contacto elige su canal y pasa su propio contacto;
 * este notificador no conoce los medios ni los destinatarios.
 *
 * <p>Un fallo de envío se registra en el log y no se propaga: el aviso es de mejor esfuerzo.
 */
public class NotificadorPorCanal {
  private static final Logger LOGGER = LoggerFactory.getLogger(NotificadorPorCanal.class);
  private static final String ASUNTO_POR_DEFECTO = "Novedad de DonaTrack";

  private final GatewayCorreo correo;
  private final GatewayMensajes sms;
  private final GatewayMensajes whatsapp;

  public NotificadorPorCanal(GatewayCorreo correo, GatewayMensajes sms, GatewayMensajes whatsapp) {
    this.correo = Objects.requireNonNull(correo);
    this.sms = Objects.requireNonNull(sms);
    this.whatsapp = Objects.requireNonNull(whatsapp);
  }

  public void enviarPorCorreo(String direccion, String mensaje) {
    enviar("CORREO", () -> correo.enviar(direccion, ASUNTO_POR_DEFECTO, mensaje));
  }

  public void enviarPorSms(String numero, String mensaje) {
    enviar("SMS", () -> sms.enviar(numero, mensaje));
  }

  public void enviarPorWhatsapp(String numero, String mensaje) {
    enviar("WHATSAPP", () -> whatsapp.enviar(numero, mensaje));
  }

  private void enviar(String canal, Supplier<ResultadoEnvio> envio) {
    try {
      ResultadoEnvio resultado = envio.get();
      if (!resultado.exitoso()) {
        LOGGER.warn("Fallo de notificación canal={} detalle={}", canal, resultado.detalle());
      }
    } catch (RuntimeException error) {
      LOGGER.warn("Fallo de notificación canal={} detalle={}", canal, error.getMessage());
    }
  }
}
