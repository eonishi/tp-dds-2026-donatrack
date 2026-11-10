package ar.edu.utn.donatrack.web.controller;

import ar.edu.utn.donatrack.beneficiarios.BeneficiariosRepository;
import ar.edu.utn.donatrack.bienes.SubcategoriasRepository;
import ar.edu.utn.donatrack.donaciones.DonacionesRepository;
import ar.edu.utn.donatrack.flota.FlotaLogistica;
import ar.edu.utn.donatrack.importacion.ImportadorDonantes;
import ar.edu.utn.donatrack.notificaciones.BandejaNotificacionesRepository;
import ar.edu.utn.donatrack.notificaciones.ServicioNotificaciones;
import ar.edu.utn.donatrack.propuestas.PropuestaRepository;
import ar.edu.utn.donatrack.propuestas.ServicioMatchmakingDonaciones;
import ar.edu.utn.donatrack.security.AuthService;
import ar.edu.utn.donatrack.seguimiento.PosicionesCamiones;
import java.time.Clock;

/** Arma los controllers de las vistas de cada rol (donante, entidad y administración). */
public final class VistasPorRol {
  private final DonanteVistasController donante;
  private final EntidadVistasController entidad;
  private final AdminVistasController admin;

  public VistasPorRol(
      AuthService authService,
      DonacionesRepository donaciones,
      BeneficiariosRepository entidades,
      SubcategoriasRepository subcategorias,
      PropuestaRepository propuestas,
      ServicioMatchmakingDonaciones matchmaking,
      ServicioNotificaciones notificaciones,
      BandejaNotificacionesRepository bandeja,
      PosicionesCamiones posiciones,
      FlotaLogistica flota,
      ImportadorDonantes importador,
      Clock clock) {
    Seguimiento seguimiento = new Seguimiento(posiciones);
    this.donante =
        new DonanteVistasController(authService, donaciones, subcategorias, bandeja, seguimiento);
    this.entidad =
        new EntidadVistasController(
            authService, entidades, subcategorias, donaciones, bandeja, seguimiento);
    this.admin =
        new AdminVistasController(
            authService,
            donaciones,
            entidades,
            propuestas,
            matchmaking,
            notificaciones,
            flota,
            importador,
            clock);
  }

  public void routes() {
    donante.routes();
    entidad.routes();
    admin.routes();
  }
}
