package ar.edu.utn.donatrack.flota;

import java.util.List;

/**
 * Administración de camiones, que vive en logística. Cada operación viaja con la sesión de la
 * persona administradora: logística decide si tiene permiso, no donaciones.
 */
public interface FlotaLogistica {
  List<Camion> camiones(String sesion);

  Camion crear(String sesion, Camion camion);

  Camion actualizar(String sesion, Camion camion);

  boolean eliminar(String sesion, String patente);
}
