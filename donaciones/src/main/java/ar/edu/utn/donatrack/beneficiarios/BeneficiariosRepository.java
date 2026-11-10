package ar.edu.utn.donatrack.beneficiarios;

import ar.edu.utn.donatrack.utilidades.Busqueda;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.util.List;
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

  /**
   * Una página de entidades, ordenada por razón social: la base filtra y pagina, no se cargan todas
   * en memoria. Los correos de la página llegan en una segunda consulta, no uno por fila.
   *
   * @param texto filtro opcional: parte de la razón social, la dirección o un correo
   * @param desde cuántas entidades saltear
   * @param cantidad cuántas devolver como máximo
   */
  public List<EntidadBeneficiaria> buscar(String texto, int desde, int cantidad) {
    var pagina =
        createQuery(
            "select e.id from EntidadBeneficiaria e"
                + filtro(texto)
                + " order by e.razonSocial, e.id",
            Long.class);
    Busqueda.patron(texto).ifPresent(p -> pagina.setParameter("patron", p));
    List<Long> ids = pagina.setFirstResult(desde).setMaxResults(cantidad).getResultList();
    if (ids.isEmpty()) {
      return List.of();
    }
    return createQuery(
            "select distinct e from EntidadBeneficiaria e left join fetch e.correos"
                + " where e.id in :ids order by e.razonSocial, e.id",
            EntidadBeneficiaria.class)
        .setParameter("ids", ids)
        .getResultList();
  }

  /**
   * @return cuántas entidades cumplen el mismo filtro que {@link #buscar}
   */
  public long contar(String texto) {
    var consulta =
        createQuery("select count(e) from EntidadBeneficiaria e" + filtro(texto), Long.class);
    Busqueda.patron(texto).ifPresent(p -> consulta.setParameter("patron", p));
    return consulta.getSingleResult();
  }

  private static String filtro(String texto) {
    if (Busqueda.patron(texto).isEmpty()) {
      return "";
    }
    return " where lower(e.razonSocial) like :patron"
        + " or lower(e.direccion) like :patron"
        + " or e.id in (select o.id from EntidadBeneficiaria o join o.correos c"
        + "   where lower(c.contacto) like :patron)";
  }
}
