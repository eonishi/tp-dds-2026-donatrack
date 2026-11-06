package ar.edu.utn.donatrack.logistica;

import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de repartidores respaldado por JPA, siguiendo el patrón de {@link CamionRepository}.
 *
 * <p>Las escrituras requieren una transacción activa del llamador. No admite dos repartidores con
 * el mismo email porque el email es la credencial de acceso.
 */
public class RepartidorRepository implements WithSimplePersistenceUnit {

  public void agregar(Repartidor repartidor) {
    if (buscarPorEmail(repartidor.getEmail()).isPresent()) {
      throw new IllegalArgumentException(
          "ya existe un repartidor con el email " + repartidor.getEmail());
    }
    persist(repartidor);
  }

  public Optional<Repartidor> buscarPorId(UUID id) {
    return Optional.ofNullable(id == null ? null : find(Repartidor.class, id));
  }

  public Optional<Repartidor> buscarPorEmail(String email) {
    if (email == null) {
      return Optional.empty();
    }
    return createQuery("select r from Repartidor r where r.email = :email", Repartidor.class)
        .setParameter("email", Repartidor.normalizarEmail(email))
        .getResultList()
        .stream()
        .findFirst();
  }

  public List<Repartidor> listarTodos() {
    return List.copyOf(createQuery("select r from Repartidor r", Repartidor.class).getResultList());
  }
}
