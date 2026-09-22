package ar.edu.utn.donatrack.notificaciones.payloads;

public sealed interface NotificacionPayload permits ComprobanteEntrega, InfoMapa, SinPayload {}
