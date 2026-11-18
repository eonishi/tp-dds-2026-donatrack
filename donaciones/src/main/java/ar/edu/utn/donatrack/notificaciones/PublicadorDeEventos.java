package ar.edu.utn.donatrack.notificaciones;

import ar.edu.utn.donatrack.notificaciones.eventos.EventoNotificable;

/**
 * Destino de los eventos que publica el dominio al transicionar.
 *
 * <p>El dominio publica en el momento en que ocurre el hecho; la implementación decide cuándo sale
 * el aviso: en el acto ({@link ServicioNotificaciones}) o al confirmarse el caso de uso ({@link
 * PublicadorDiferido}).
 */
public interface PublicadorDeEventos {
  /**
   * Publica un evento ocurrido.
   *
   * @param evento hecho ocurrido
   */
  void publicar(EventoNotificable evento);
}
