package ar.edu.utn.donatrack.notificaciones.payloads;

import ar.edu.utn.donatrack.logistica.Camion;
import java.time.LocalDateTime;

public record ComprobanteEntrega(LocalDateTime fecha, Camion camion)
    implements NotificacionPayload {}
