package ar.edu.utn.donatrack.donantes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class DonantesRepository {
  private final Map<UUID, Donante> donantes = new LinkedHashMap<>();

  public synchronized Set<Donante> getDonantes() {
    return Collections.unmodifiableSet(Set.copyOf(donantes.values()));
  }

  public synchronized void addMany(List<Donante> nuevos) {
    nuevos.forEach(this::save);
  }

  // Update o Insert
  public synchronized void upsert(List<Donante> entrantes) {
    if (entrantes == null || entrantes.isEmpty()) {
      return;
    }
    for (Donante entrante : entrantes) {
      Optional<String> email = entrante.extraerEmail();
      // Defensive: if no email, insert (shouldn't happen after constructor change)
      if (email.isEmpty()) {
        save(entrante);
        continue;
      }
      Donante existente = buscarPorEmail(email.get());
      if (existente != null) {
        existente.merge(entrante);
      } else {
        save(entrante);
      }
    }
  }

  public synchronized Donante save(Donante donante) {
    donantes.put(donante.getId(), donante);
    return donante;
  }

  public synchronized Optional<Donante> findById(UUID id) {
    return Optional.ofNullable(donantes.get(id));
  }

  public synchronized void deleteById(UUID id) {
    donantes.remove(id);
  }

  private Donante buscarPorEmail(String email) {
    for (Donante d : donantes.values()) {
      if (d.extraerEmail().map(e -> e.equals(email)).orElse(false)) {
        return d;
      }
    }
    return null;
  }
}
