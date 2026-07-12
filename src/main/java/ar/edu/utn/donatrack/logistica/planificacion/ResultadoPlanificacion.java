package ar.edu.utn.donatrack.logistica.planificacion;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.logistica.Camion;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ResultadoPlanificacion(LoteId loteId, Map<Camion, List<EntidadBeneficiaria>> rutas) {
  public ResultadoPlanificacion {
    Objects.requireNonNull(loteId, "loteId no debe ser null");
    Objects.requireNonNull(rutas, "rutas no debe ser null");
  }
}
