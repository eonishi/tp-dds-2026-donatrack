package ar.edu.utn.donatrack.beneficiarios;

import ar.edu.utn.donatrack.persistencia.JpaRepository;
import java.util.Set;

/**
 * JPA-backed repository of beneficiary entities: reads query the database and writes persist there.
 */
public class BeneficiariosRepository implements JpaRepository {

  /**
   * @return an immutable snapshot of every stored beneficiary entity.
   */
  public Set<EntidadBeneficiaria> getBeneficiarios() {
    return Set.copyOf(
        createQuery("select e from EntidadBeneficiaria e", EntidadBeneficiaria.class)
            .getResultList());
  }

  /**
   * Registers a beneficiary entity. If one with the same {@code razonSocial} — which is what {@link
   * EntidadBeneficiaria#equals} compares — is already stored, the stored one is kept and the new
   * instance is ignored, as the previous in-memory {@code HashSet} did.
   *
   * @param nuevo the beneficiary entity to register
   */
  public void addBeneficiario(EntidadBeneficiaria nuevo) {
    inTransaction(
        () -> {
          if (!estaRegistrada(nuevo.getRazonSocial())) {
            persist(nuevo);
          }
        });
  }

  private boolean estaRegistrada(String razonSocial) {
    return !createQuery(
            "select e from EntidadBeneficiaria e where e.razonSocial = :razon",
            EntidadBeneficiaria.class)
        .setParameter("razon", razonSocial)
        .getResultList()
        .isEmpty();
  }
}
