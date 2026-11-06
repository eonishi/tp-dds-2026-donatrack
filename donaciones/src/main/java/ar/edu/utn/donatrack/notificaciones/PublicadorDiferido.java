package ar.edu.utn.donatrack.notificaciones;

import ar.edu.utn.donatrack.notificaciones.eventos.EventoNotificable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Publicador que retiene los eventos hasta que el caso de uso se confirma.
 *
 * <p>Se usa uno por caso de uso transaccional: el dominio publica durante la transacción y el
 * llamador invoca {@link #enviarPendientes()} recién después del commit. Si la transacción se
 * revierte, los eventos retenidos se descartan con el publicador y no sale ningún aviso.
 */
public class PublicadorDiferido implements PublicadorDeEventos {
  private final ServicioNotificaciones notificaciones;
  private final List<EventoNotificable> pendientes = new ArrayList<>();

  PublicadorDiferido(ServicioNotificaciones notificaciones) {
    this.notificaciones = Objects.requireNonNull(notificaciones);
  }

  @Override
  public void publicar(EventoNotificable evento) {
    pendientes.add(Objects.requireNonNull(evento));
  }

  /** Envía los eventos retenidos en el orden en que se publicaron y los olvida. */
  public void enviarPendientes() {
    List<EventoNotificable> porEnviar = List.copyOf(pendientes);
    pendientes.clear();
    porEnviar.forEach(notificaciones::publicar);
  }
}
