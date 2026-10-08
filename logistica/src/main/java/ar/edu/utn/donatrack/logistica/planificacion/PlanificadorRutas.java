package ar.edu.utn.donatrack.logistica.planificacion;

import java.util.UUID;

public interface PlanificadorRutas {
  UUID enviarLote(LoteDonaciones lote);
}
