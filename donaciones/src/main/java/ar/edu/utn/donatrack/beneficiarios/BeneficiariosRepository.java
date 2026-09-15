package ar.edu.utn.donatrack.beneficiarios;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class BeneficiariosRepository {
  private final Map<UUID, EntidadBeneficiaria> beneficiarios = new LinkedHashMap<>();

  public synchronized Set<EntidadBeneficiaria> getBeneficiarios() {
    return Set.copyOf(beneficiarios.values());
  }

  public synchronized void addBeneficiario(EntidadBeneficiaria nuevo) {
    save(nuevo);
  }

  public synchronized EntidadBeneficiaria save(EntidadBeneficiaria entidad) {
    beneficiarios.put(entidad.getId(), entidad);
    return entidad;
  }

  public synchronized Optional<EntidadBeneficiaria> findById(UUID id) {
    return Optional.ofNullable(beneficiarios.get(id));
  }

  public synchronized void deleteById(UUID id) {
    beneficiarios.remove(id);
  }
}
