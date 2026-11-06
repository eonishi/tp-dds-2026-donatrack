package ar.edu.utn.donatrack.testsupport;

import ar.edu.utn.donatrack.flota.Camion;
import ar.edu.utn.donatrack.flota.FlotaLogistica;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Logística de mentira para las vistas de camiones: guarda la flota y la sesión recibida. */
public class FlotaEnMemoria implements FlotaLogistica {
  private final Map<String, Camion> camiones = new LinkedHashMap<>();
  private final List<String> sesiones = new ArrayList<>();

  @Override
  public List<Camion> camiones(String sesion) {
    sesiones.add(sesion);
    return List.copyOf(camiones.values());
  }

  @Override
  public Camion crear(String sesion, Camion camion) {
    sesiones.add(sesion);
    if (camiones.containsKey(camion.getPatente())) {
      throw new IllegalArgumentException(
          "Ya existe un camión con la patente " + camion.getPatente());
    }
    camiones.put(camion.getPatente(), camion);
    return camion;
  }

  @Override
  public Camion actualizar(String sesion, Camion camion) {
    sesiones.add(sesion);
    camiones.put(camion.getPatente(), camion);
    return camion;
  }

  @Override
  public boolean eliminar(String sesion, String patente) {
    sesiones.add(sesion);
    return camiones.remove(patente) != null;
  }

  public Map<String, Camion> flota() {
    return camiones;
  }

  public List<String> sesiones() {
    return sesiones;
  }
}
