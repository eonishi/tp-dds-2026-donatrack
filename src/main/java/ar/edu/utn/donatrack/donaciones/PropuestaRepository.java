package ar.edu.utn.donatrack.donaciones;

import java.util.List;
import java.util.Optional;

public interface PropuestaRepository {
  Optional<PropuestaDeAsignacion> findPendingByDonacion(Donacion donacion);

  List<PropuestaDeAsignacion> findAllPending();

  void save(PropuestaDeAsignacion propuesta);

  void remove(Donacion donacion);
}
