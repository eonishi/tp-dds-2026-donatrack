package ar.edu.utn.donatrack.notificaciones;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Gateways de envío por canal. Cada medio de contacto elige su canal y pasa su propio contacto;
 * este notificador no conoce los medios ni los destinatarios.
 */
public class NotificadorPorCanal {
  private static final String ASUNTO_POR_DEFECTO = "Novedad de DonaTrack";

  private final GatewayCorreo correo;
  private final GatewaySms sms;
  private final GatewayWhatsapp whatsapp;

  public NotificadorPorCanal(GatewayCorreo correo, GatewaySms sms, GatewayWhatsapp whatsapp) {
    this.correo = Objects.requireNonNull(correo);
    this.sms = Objects.requireNonNull(sms);
    this.whatsapp = Objects.requireNonNull(whatsapp);
  }

  public Envio enviarPorCorreo(String direccion, String mensaje) {
    return enviar(Canal.CORREO, () -> correo.enviar(direccion, ASUNTO_POR_DEFECTO, mensaje));
  }

  public Envio enviarPorSms(String numero, String mensaje) {
    return enviar(Canal.SMS, () -> sms.enviar(numero, mensaje));
  }

  public Envio enviarPorWhatsapp(String numero, String mensaje) {
    return enviar(Canal.WHATSAPP, () -> whatsapp.enviar(numero, mensaje));
  }

  private Envio enviar(Canal canal, Supplier<ResultadoEnvio> envio) {
    try {
      return new Envio(canal, envio.get());
    } catch (RuntimeException error) {
      return new Envio(
          canal,
          ResultadoEnvio.fallido(
              error.getMessage() == null ? "Error inesperado." : error.getMessage()));
    }
  }
}
