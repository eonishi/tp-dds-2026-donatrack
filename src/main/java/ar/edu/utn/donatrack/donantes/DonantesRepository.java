package ar.edu.utn.donatrack.donantes;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class DonantesRepository {
  private final Set<Donante> donantes = new HashSet<>();

  public Set<Donante> getDonantes() {
    return Collections.unmodifiableSet(donantes);
  }

  public void addMany(List<Donante> nuevos) {
    donantes.addAll(nuevos);
  }

  // Update o Insert
  public void upsert(List<Donante> entrantes) {
    if (entrantes == null || entrantes.isEmpty()) {
      return;
    }
    for (Donante entrante : entrantes) {
      Optional<String> email = entrante.extraerEmail();
      // Defensive: if no email, insert (shouldn't happen after constructor change)
      if (email.isEmpty()) {
        donantes.add(entrante);
        continue;
      }
      Donante existente = buscarPorEmail(email.get());
      if (existente != null) {
        existente.merge(entrante);
      } else {
        donantes.add(entrante);
      }
    }
  }

  private Donante buscarPorEmail(String email) {
    for (Donante d : donantes) {
      if (d.extraerEmail().map(e -> e.equals(email)).orElse(false)) {
        return d;
      }
    }
    return null;
  }
}
