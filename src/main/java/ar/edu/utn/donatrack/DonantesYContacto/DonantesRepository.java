package ar.edu.utn.donatrack.DonantesYContacto;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DonantesRepository {
  private final Set<Donante> donantes = new HashSet<>();

  public Set<Donante> getDonantes() {
    return Collections.unmodifiableSet(donantes);
  }

  public void addMany(List<Donante> nuevos) {
    donantes.addAll(nuevos);
  }

}