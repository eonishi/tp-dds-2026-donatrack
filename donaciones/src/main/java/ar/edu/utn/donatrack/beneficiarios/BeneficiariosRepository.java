package ar.edu.utn.donatrack.beneficiarios;

import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.util.Optional;
import java.util.Set;

/**
 * Repositorio de entidades beneficiarias respaldado por JPA: las lecturas consultan la base y las
 * escrituras persisten en ella, conservando la API que usan los controladores y el matchmaking.
 */
public class BeneficiariosRepository implements WithSimplePersistenceUnit {

  /**
   * @return una instantánea inmutable de todas las entidades beneficiarias almacenadas.
   */
  public Set<EntidadBeneficiaria> getBeneficiarios() {
    return Set.copyOf(
        createQuery("select e from EntidadBeneficiaria e", EntidadBeneficiaria.class)
            .getResultList());
  }

  /**
   * Registra una entidad beneficiaria. Equivale a {@link #save(EntidadBeneficiaria)}: la identidad
   * de la entidad decide si la fila se inserta o se actualiza.
   *
   * @param nuevo la entidad beneficiaria a registrar
   */
  public void addBeneficiario(EntidadBeneficiaria nuevo) {
    save(nuevo);
  }

  /**
   * Inserta o actualiza la entidad beneficiaria junto con sus necesidades y sus correos. Si todavía
   * no existe, la inserta; si ya está administrada por el contexto de persistencia, la devuelve
   * para que el vaciado de la transacción guarde sus cambios; si está separada, fusiona su estado
   * con la fila almacenada.
   *
   * <p>Requiere una transacción activa del llamador (el caso de uso); nunca abre ni confirma una
   * transacción por su cuenta.
   *
   * <p>La subcategoría de cada necesidad debe existir y llegar administrada por {@code
   * SubcategoriasRepository}, el único repositorio que escribe el catálogo: este repositorio nunca
   * crea entradas del catálogo como efecto lateral de guardar la entidad.
   *
   * @param entidad la entidad beneficiaria a guardar
   * @return la instancia administrada resultante
   */
  public EntidadBeneficiaria save(EntidadBeneficiaria entidad) {
    if (entityManager().contains(entidad)) {
      return entidad;
    }
    return merge(entidad);
  }

  /**
   * Busca una entidad beneficiaria por su identidad.
   *
   * @param id identidad de la entidad beneficiaria
   * @return la entidad almacenada, o {@link Optional#empty()} si no existe
   */
  public Optional<EntidadBeneficiaria> findById(Long id) {
    return Optional.ofNullable(find(EntidadBeneficiaria.class, id));
  }

  /**
   * Elimina la entidad beneficiaria indicada junto con sus necesidades y sus correos. Si no existe,
   * no hace nada.
   *
   * <p>Requiere una transacción activa del llamador (el caso de uso); nunca abre ni confirma una
   * transacción por su cuenta.
   *
   * @param id identidad de la entidad beneficiaria
   */
  public void deleteById(Long id) {
    EntidadBeneficiaria entidad = find(EntidadBeneficiaria.class, id);
    if (entidad != null) {
      remove(entidad);
    }
  }
}
