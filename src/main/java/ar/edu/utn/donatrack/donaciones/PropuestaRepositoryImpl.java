package ar.edu.utn.donatrack.donaciones;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PropuestaRepositoryImpl implements PropuestaRepository {
  private final Map<Donacion, PropuestaDeAsignacion> propuestas = new HashMap<>();

  @Override
  public Optional<PropuestaDeAsignacion> findPendingByDonacion(Donacion donacion) {
    PropuestaDeAsignacion propuesta = propuestas.get(donacion);
    if (propuesta != null && propuesta.isPendiente()) {
      return Optional.of(propuesta);
    }
    return Optional.empty();
  }

  @Override
  public List<PropuestaDeAsignacion> findAllPending() {
    return propuestas.values().stream().filter(PropuestaDeAsignacion::isPendiente).toList();
  }

  @Override
  public void save(PropuestaDeAsignacion propuesta) {
    propuestas.put(propuesta.getDonacion(), propuesta);
  }

  @Override
  public void remove(Donacion donacion) {
    propuestas.remove(donacion);
  }
}
