package ar.edu.utn.donatrack.donantes;

import ar.edu.utn.donatrack.persistencia.JpaRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** JPA-backed repository of donors: every read queries the database, every write persists there. */
public class DonantesRepository implements JpaRepository {

  /**
   * @return an immutable snapshot of every stored donor.
   */
  public Set<Donante> getDonantes() {
    return Set.copyOf(createQuery("select d from Donante d", Donante.class).getResultList());
  }

  /**
   * Persists every donor of the batch inside a single transaction.
   *
   * @param nuevos the donors to register
   */
  public void addMany(List<Donante> nuevos) {
    inTransaction(() -> nuevos.forEach(this::persist));
  }

  /**
   * Inserts the incoming donors that are not stored yet, and merges the domain data of the ones
   * that already exist, matching by email.
   *
   * @param entrantes the donors to upsert
   */
  public void upsert(List<Donante> entrantes) {
    if (entrantes == null || entrantes.isEmpty()) {
      return;
    }
    inTransaction(
        () -> {
          // The whole donor table is loaded on purpose: the previous in-memory implementation
          // matched each incoming donor against every stored donor by email, and reproducing that
          // matching semantics exactly requires the same in-memory pass. The per-call cost is
          // acceptable for this import flow, which runs off the operator's schedule.
          List<Donante> existentes =
              new ArrayList<>(
                  createQuery("select d from Donante d", Donante.class).getResultList());
          for (Donante entrante : entrantes) {
            upsertOne(entrante, existentes);
          }
        });
  }

  private void upsertOne(Donante entrante, List<Donante> existentes) {
    Optional<String> email = entrante.extraerEmail();
    // Defensive: if no email, insert (shouldn't happen after constructor change)
    if (email.isEmpty()) {
      insertar(entrante, existentes);
      return;
    }
    Donante existente = buscarPorEmail(existentes, email.get());
    if (existente != null) {
      existente.merge(entrante);
    } else {
      insertar(entrante, existentes);
    }
  }

  private void insertar(Donante entrante, List<Donante> existentes) {
    persist(entrante);
    // The in-memory view stays current so a later donor of the same batch can still match it.
    existentes.add(entrante);
  }

  private static Donante buscarPorEmail(List<Donante> donantes, String email) {
    for (Donante d : donantes) {
      if (d.extraerEmail().map(e -> e.equals(email)).orElse(false)) {
        return d;
      }
    }
    return null;
  }
}
