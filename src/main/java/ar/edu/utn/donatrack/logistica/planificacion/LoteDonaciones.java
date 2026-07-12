package ar.edu.utn.donatrack.logistica.planificacion;

import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.logistica.Camion;
import java.util.List;
import java.util.Objects;

public record LoteDonaciones(List<Donacion> donaciones, List<Camion> camiones) {
  public LoteDonaciones {
    Objects.requireNonNull(donaciones, "donaciones no debe ser null");
    Objects.requireNonNull(camiones, "camiones no debe ser null");
    if (camiones.isEmpty()) {
      throw new IllegalArgumentException("lote debe tener al menos un camión");
    }
    if (donaciones.size() > 100) {
      throw new IllegalArgumentException("lote no puede exceder 100 donaciones");
    }
  }
}
