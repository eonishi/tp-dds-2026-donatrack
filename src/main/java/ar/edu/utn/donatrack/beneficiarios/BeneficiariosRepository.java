package ar.edu.utn.donatrack.beneficiarios;

import ar.edu.utn.donatrack.persistencia.JpaRepository;
import java.util.Set;

/**
 * Repositorio de entidades beneficiarias respaldado por JPA: las lecturas consultan la base y las
 * escrituras persisten en ella.
 */
public class BeneficiariosRepository implements JpaRepository {

  /**
   * @return una instantánea inmutable de todas las entidades beneficiarias almacenadas.
   */
  public Set<EntidadBeneficiaria> getBeneficiarios() {
    return Set.copyOf(
        createQuery("select e from EntidadBeneficiaria e", EntidadBeneficiaria.class)
            .getResultList());
  }

  /**
   * Registra una entidad beneficiaria. Si ya hay una almacenada con la misma {@code razonSocial}
   * —que es lo que compara {@link EntidadBeneficiaria#equals}—, se conserva la almacenada y se
   * ignora la instancia nueva, como hacía el {@code HashSet} en memoria anterior.
   *
   * @param nuevo la entidad beneficiaria a registrar
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
