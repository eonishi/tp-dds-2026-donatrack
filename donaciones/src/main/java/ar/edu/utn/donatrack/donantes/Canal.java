package ar.edu.utn.donatrack.donantes;

import ar.edu.utn.donatrack.notificaciones.NotificadorPorCanal;

/**
 * Por dónde se le escribe a un {@link MedioDeContacto}. Cada canal sabe qué gateway usar y qué
 * forma tiene un contacto válido, así que sumar uno nuevo es agregar una constante acá.
 */
public enum Canal {
  CORREO {
    @Override
    void enviar(String contacto, String mensaje, NotificadorPorCanal canales) {
      canales.enviarPorCorreo(contacto, mensaje);
    }

    @Override
    String normalizar(String contacto) {
      String correo = obligatorio(contacto, "Falta el correo electrónico.").trim();
      if (!correo.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
        throw new IllegalArgumentException("El correo electrónico no es válido.");
      }
      return correo;
    }
  },
  SMS {
    @Override
    void enviar(String contacto, String mensaje, NotificadorPorCanal canales) {
      canales.enviarPorSms(contacto, mensaje);
    }

    @Override
    String normalizar(String contacto) {
      return telefono(contacto);
    }
  },
  WHATSAPP {
    @Override
    void enviar(String contacto, String mensaje, NotificadorPorCanal canales) {
      canales.enviarPorWhatsapp(contacto, mensaje);
    }

    @Override
    String normalizar(String contacto) {
      return telefono(contacto);
    }
  };

  abstract void enviar(String contacto, String mensaje, NotificadorPorCanal canales);

  /**
   * El contacto tal como se guarda.
   *
   * @throws IllegalArgumentException si falta o no tiene la forma que pide el canal
   */
  abstract String normalizar(String contacto);

  // Se aceptan espacios, guiones, puntos y paréntesis ("+54 11 5181-9600"); se guarda en E.164.
  private static String telefono(String contacto) {
    String numero = obligatorio(contacto, "Falta el teléfono.").replaceAll("[\\s.()-]", "");
    if (!numero.matches("\\+[1-9][0-9]{1,14}")) {
      throw new IllegalArgumentException(
          "El teléfono debe usar formato internacional E.164, por ejemplo +5491155555555.");
    }
    return numero;
  }

  private static String obligatorio(String valor, String mensaje) {
    if (valor == null || valor.isBlank()) {
      throw new IllegalArgumentException(mensaje);
    }
    return valor;
  }
}
