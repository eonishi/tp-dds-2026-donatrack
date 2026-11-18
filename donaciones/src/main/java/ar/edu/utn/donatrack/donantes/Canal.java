package ar.edu.utn.donatrack.donantes;

import ar.edu.utn.donatrack.notificaciones.NotificadorPorCanal;

/**
 * Por dónde se le escribe a un {@link MedioDeContacto}. Cada canal sabe qué gateway usar, así que
 * sumar uno nuevo es agregar una constante acá.
 */
public enum Canal {
  CORREO {
    @Override
    void enviar(String contacto, String mensaje, NotificadorPorCanal canales) {
      canales.enviarPorCorreo(contacto, mensaje);
    }
  },
  SMS {
    @Override
    void enviar(String contacto, String mensaje, NotificadorPorCanal canales) {
      canales.enviarPorSms(contacto, mensaje);
    }
  },
  WHATSAPP {
    @Override
    void enviar(String contacto, String mensaje, NotificadorPorCanal canales) {
      canales.enviarPorWhatsapp(contacto, mensaje);
    }
  };

  abstract void enviar(String contacto, String mensaje, NotificadorPorCanal canales);
}
