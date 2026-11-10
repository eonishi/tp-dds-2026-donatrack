package ar.edu.utn.donatrack.security;

import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de usuarios respaldado por JPA.
 *
 * <p>Las escrituras requieren una transacción activa del llamador, igual que el resto de los
 * repositorios del módulo.
 */
public class UsuariosRepository implements WithSimplePersistenceUnit {

  public Optional<Usuario> buscarPorEmail(String email) {
    if (email == null) {
      return Optional.empty();
    }
    return createQuery("select u from Usuario u where u.email = :email", Usuario.class)
        .setParameter("email", email.trim().toLowerCase())
        .getResultList()
        .stream()
        .findFirst();
  }

  public Optional<Usuario> buscarPorId(UUID id) {
    return Optional.ofNullable(id == null ? null : find(Usuario.class, id));
  }

  public boolean existeEmail(String email) {
    return buscarPorEmail(email).isPresent();
  }

  /** Los usuarios vinculados a esas entidades beneficiarias, en una sola consulta. */
  public List<Usuario> buscarPorEntidades(Collection<Long> entidades) {
    if (entidades.isEmpty()) {
      return List.of();
    }
    return createQuery("select u from Usuario u where u.entidadId in :ids", Usuario.class)
        .setParameter("ids", entidades)
        .getResultList();
  }

  /** Los usuarios vinculados a esos donantes, en una sola consulta. */
  public List<Usuario> buscarPorDonantes(Collection<UUID> donantes) {
    if (donantes.isEmpty()) {
      return List.of();
    }
    return createQuery("select u from Usuario u where u.donanteId in :ids", Usuario.class)
        .setParameter("ids", donantes)
        .getResultList();
  }

  public void agregar(Usuario usuario) {
    persist(usuario);
  }
}
