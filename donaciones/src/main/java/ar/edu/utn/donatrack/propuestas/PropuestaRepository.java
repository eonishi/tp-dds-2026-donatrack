package ar.edu.utn.donatrack.propuestas;

import java.util.List;
import java.util.Optional;

import ar.edu.utn.donatrack.donaciones.Donacion;

public interface PropuestaRepository {
  Optional<PropuestaDeAsignacion> findPendingByDonacion(Donacion donacion);

  List<PropuestaDeAsignacion> findAllPending();

  void save(PropuestaDeAsignacion propuesta);

  void remove(Donacion donacion);
}
