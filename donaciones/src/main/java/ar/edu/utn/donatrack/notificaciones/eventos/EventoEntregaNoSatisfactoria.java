package ar.edu.utn.donatrack.notificaciones.eventos;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.notificaciones.Notificable;
import ar.edu.utn.donatrack.notificaciones.Notificacion;
import ar.edu.utn.donatrack.notificaciones.payloads.SinPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EventoEntregaNoSatisfactoria extends EventoNotificable {
  private final Donante donante;
  private final EntidadBeneficiaria entidad;
  private final String motivo;

  public EventoEntregaNoSatisfactoria(Donante donante, EntidadBeneficiaria entidad, String motivo) {
    this.donante = Objects.requireNonNull(donante);
    this.entidad = Objects.requireNonNull(entidad);
    this.motivo = Objects.requireNonNull(motivo);
  }

  @Override
  public List<Notificacion> generarNotificaciones() {
    List<Notificacion> notificaciones = new ArrayList<>();
    String mensajeDonante =
        "La entrega a " + entidad.getRazonSocial() + " no pudo completarse: " + motivo;
    notificaciones.add(new Notificacion(donante, mensajeDonante, SinPayload.INSTANCE));
    String mensajeEntidad = "La entrega no pudo completarse: " + motivo;
    notificaciones.add(new Notificacion(entidad, mensajeEntidad, SinPayload.INSTANCE));

    String adminConfig =
        System.getProperty("donatrack.admin.emails", System.getenv("DONATRACK_ADMIN_EMAILS"));
    if (adminConfig != null && !adminConfig.isBlank()) {
      String mensajeAdmin =
          "Alerta: La entrega a "
              + entidad.getRazonSocial()
              + " no pudo completarse. Motivo: "
              + motivo;
      for (String email : adminConfig.split(",")) {
        notificaciones.add(
            new Notificacion(administrador(email.trim()), mensajeAdmin, SinPayload.INSTANCE));
      }
    }
    return notificaciones;
  }

  // Los administradores todavía no son parte del dominio: solo se conoce su correo de config.
  private static Notificable administrador(String email) {
    CorreoElectronico correo = new CorreoElectronico(email);
    return (mensaje, canales) -> List.of(correo.enviar(mensaje, canales));
  }
}
