package ar.edu.utn.donatrack.planificacion;

import java.util.Objects;
import java.util.UUID;

public record LoteId(UUID value) {
  public LoteId {
    Objects.requireNonNull(value, "value no debe ser null");
  }

  public static LoteId generar() {
    return new LoteId(UUID.randomUUID());
  }
}
