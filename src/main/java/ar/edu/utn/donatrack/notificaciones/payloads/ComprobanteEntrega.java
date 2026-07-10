package ar.edu.utn.donatrack.notificaciones.payloads;

import java.time.LocalDateTime;

public record ComprobanteEntrega(LocalDateTime fecha) implements NotificacionPayload {}
