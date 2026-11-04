package ar.edu.utn.donatrack.propuestas;

import ar.edu.utn.donatrack.donaciones.Donacion;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.util.List;
import java.util.Optional;

public class PropuestaRepositoryImpl implements PropuestaRepository, WithSimplePersistenceUnit {

  @Override
  public Optional<PropuestaDeAsignacion> findPendingByDonacion(Donacion donacion) {
    if (donacion == null || donacion.getId() == null) {
      return Optional.empty();
    }
    return createQuery(
            "SELECT p FROM PropuestaDeAsignacion p"
                + " WHERE p.donacion.id = :donacionId AND p.pendiente = true",
            PropuestaDeAsignacion.class)
        .setParameter("donacionId", donacion.getId())
        .getResultStream()
        .findFirst();
  }

  @Override
  public List<PropuestaDeAsignacion> findAllPending() {
    return List.copyOf(
        createQuery(
                "SELECT p FROM PropuestaDeAsignacion p WHERE p.pendiente = true",
                PropuestaDeAsignacion.class)
            .getResultList());
  }

  @Override
  public void save(PropuestaDeAsignacion propuesta) {
    Optional<PropuestaDeAsignacion> existente = findByDonacion(propuesta.getDonacion());
    if (existente.isPresent()) {
      PropuestaDeAsignacion actual = existente.get();
      if (actual != propuesta) {
        actual.reemplazarCandidatos(propuesta.getCandidatos());
      }
      return;
    }
    persist(propuesta);
  }

  @Override
  public void remove(Donacion donacion) {
    findByDonacion(donacion).ifPresent(this::remove);
  }

  private Optional<PropuestaDeAsignacion> findByDonacion(Donacion donacion) {
    if (donacion == null || donacion.getId() == null) {
      return Optional.empty();
    }
    return createQuery(
            "SELECT p FROM PropuestaDeAsignacion p WHERE p.donacion.id = :donacionId",
            PropuestaDeAsignacion.class)
        .setParameter("donacionId", donacion.getId())
        .getResultStream()
        .findFirst();
  }
}
