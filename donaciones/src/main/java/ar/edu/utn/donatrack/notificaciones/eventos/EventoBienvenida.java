package ar.edu.utn.donatrack.notificaciones.eventos;

import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.notificaciones.Notificacion;
import ar.edu.utn.donatrack.notificaciones.payloads.SinPayload;
import java.util.List;
import java.util.Objects;

public class EventoBienvenida extends EventoNotificable {
  private final Donante donante;

  public EventoBienvenida(Donante donante) {
    this.donante = Objects.requireNonNull(donante);
  }

  @Override
  public List<Notificacion> generarNotificaciones() {
    return List.of(
        new Notificacion(
            donante,
            "¡Bienvenido a DonaTrack! Gracias por registrarte como donante.",
            SinPayload.INSTANCE));
  }
}
