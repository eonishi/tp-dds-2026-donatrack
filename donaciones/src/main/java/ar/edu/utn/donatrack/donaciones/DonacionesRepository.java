package ar.edu.utn.donatrack.donaciones;

import ar.edu.utn.donatrack.estados.EnDeposito;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class DonacionesRepository {
  private final Map<UUID, Donacion> donaciones = new LinkedHashMap<>();

  public synchronized List<Donacion> getDonaciones() {
    return Collections.unmodifiableList(new ArrayList<>(donaciones.values()));
  }

  public List<Donacion> getDonacionesEnDeposito() {
    return donaciones.values().stream()
        .filter(donacion -> donacion.getEstado() instanceof EnDeposito)
        .toList();
  }

  public List<Donacion> getDonacionesEnAsignacionRealizada() {
    return donaciones.values().stream()
        .filter(donacion -> donacion.getNombreEstado() == NombreEstado.ASIGNACION_REALIZADA)
        .toList();
  }

  public synchronized void addDonacion(Donacion donacion) {
    save(donacion);
  }

  public synchronized Donacion save(Donacion donacion) {
    donaciones.put(donacion.getId(), donacion);
    return donacion;
  }

  public synchronized Optional<Donacion> findById(UUID id) {
    return Optional.ofNullable(donaciones.get(id));
  }

  public synchronized void deleteById(UUID id) {
    donaciones.remove(id);
  }
}
