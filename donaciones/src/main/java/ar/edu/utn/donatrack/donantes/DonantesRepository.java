package ar.edu.utn.donatrack.donantes;

import ar.edu.utn.donatrack.utilidades.Busqueda;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Repositorio de donantes respaldado por JPA: toda lectura consulta la base y toda escritura
 * persiste en ella. Es el repositorio dueño del agregado, así que sus métodos escriben el donante
 * completo —las personas humanas con sus medios de contacto y las jurídicas además con sus
 * representantes— y las donaciones que lo referencian reciben la instancia administrada.
 */
public class DonantesRepository implements WithSimplePersistenceUnit {

  /**
   * @return una instantánea inmutable de todos los donantes almacenados.
   */
  public Set<Donante> getDonantes() {
    return Set.copyOf(createQuery("select d from Donante d", Donante.class).getResultList());
  }

  /**
   * Persiste todos los donantes del lote.
   *
   * <p>Requiere una transacción activa del llamador (el caso de uso); nunca abre ni confirma una
   * transacción por su cuenta, así que el lote completo se une a la transacción del llamador.
   *
   * @param nuevos los donantes a registrar
   */
  public void addMany(List<Donante> nuevos) {
    nuevos.forEach(this::persist);
  }

  /**
   * Inserta los donantes entrantes que todavía no están almacenados y fusiona los datos de dominio
   * de los que ya existen, emparejando por correo electrónico.
   *
   * <p>Requiere una transacción activa del llamador (el caso de uso); nunca abre ni confirma una
   * transacción por su cuenta.
   *
   * @param entrantes los donantes a insertar o actualizar
   */
  public void upsert(List<Donante> entrantes) {
    if (entrantes == null || entrantes.isEmpty()) {
      return;
    }
    // La tabla completa de donantes se carga a propósito: la implementación en memoria
    // anterior emparejaba cada donante entrante contra todos los donantes almacenados por
    // correo electrónico, y reproducir exactamente esa semántica de emparejamiento requiere
    // la misma pasada en memoria. El costo por llamada es aceptable para este flujo de
    // importación, que corre según la agenda del operador.
    List<Donante> existentes =
        new ArrayList<>(createQuery("select d from Donante d", Donante.class).getResultList());
    for (Donante entrante : entrantes) {
      upsertOne(entrante, existentes);
    }
  }

  private void upsertOne(Donante entrante, List<Donante> existentes) {
    Optional<String> email = entrante.extraerEmail();
    // Defensivo: si no hay correo electrónico, insertar (no debería pasar tras el cambio del
    // constructor)
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
    // La vista en memoria se mantiene al día para que un donante posterior del mismo lote todavía
    // pueda emparejarlo.
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

  /**
   * Inserta o actualiza el donante junto con sus medios de contacto y sus representantes. Si
   * todavía no existe, lo inserta; si ya está administrado por el contexto de persistencia, lo
   * devuelve para que el vaciado de la transacción guarde sus cambios; si está separado, fusiona su
   * estado con la fila almacenada.
   *
   * <p>Requiere una transacción activa del llamador (el caso de uso); nunca abre ni confirma una
   * transacción por su cuenta.
   *
   * @param donante el donante a guardar
   * @return la instancia administrada resultante
   */
  public Donante save(Donante donante) {
    if (entityManager().contains(donante)) {
      return donante;
    }
    return merge(donante);
  }

  /**
   * Busca un donante por su identidad.
   *
   * @param id identidad del donante
   * @return el donante almacenado, o {@link Optional#empty()} si no existe
   */
  public Optional<Donante> findById(UUID id) {
    return Optional.ofNullable(find(Donante.class, id));
  }

  /**
   * Elimina el donante indicado junto con sus medios de contacto y sus representantes. Si no
   * existe, no hace nada.
   *
   * <p>Requiere una transacción activa del llamador (el caso de uso); nunca abre ni confirma una
   * transacción por su cuenta.
   *
   * @param id identidad del donante
   */
  public void deleteById(UUID id) {
    Donante donante = find(Donante.class, id);
    if (donante != null) {
      remove(donante);
    }
  }

  /**
   * @return si ya hay un donante con ese tipo y número de documento
   */
  public boolean existeDocumento(TipoDocumento tipo, String numero) {
    return !createQuery(
            "select d.id from Donante d where d.documento = :tipo and d.numDocumento = :numero",
            UUID.class)
        .setParameter("tipo", tipo)
        .setParameter("numero", numero)
        .setMaxResults(1)
        .getResultList()
        .isEmpty();
  }

  /**
   * @return si algún donante ya tiene ese correo electrónico como medio de contacto
   */
  public boolean existeCorreo(String correo) {
    return !createQuery(
            "select d.id from Donante d join d.mediosDeContacto m"
                + " where type(m) = CorreoElectronico and lower(m.contacto) = :correo",
            UUID.class)
        .setParameter("correo", correo.trim().toLowerCase())
        .setMaxResults(1)
        .getResultList()
        .isEmpty();
  }

  /**
   * Una página de donantes, ordenada por documento, para listados con miles de filas (por ejemplo
   * los que entraron por CSV): la base filtra y pagina, no se cargan todos en memoria.
   *
   * @param texto filtro opcional: parte del nombre, la razón social, el documento o un contacto
   * @param desde cuántos donantes saltear
   * @param cantidad cuántos devolver como máximo
   */
  public List<Donante> buscar(String texto, int desde, int cantidad) {
    var consulta =
        createQuery(
            "select d from Donante d" + filtro(texto) + " order by d.numDocumento, d.id",
            Donante.class);
    Busqueda.patron(texto).ifPresent(p -> consulta.setParameter("patron", p));
    return consulta.setFirstResult(desde).setMaxResults(cantidad).getResultList();
  }

  /**
   * @return cuántos donantes cumplen el mismo filtro que {@link #buscar}
   */
  public long contar(String texto) {
    var consulta = createQuery("select count(d) from Donante d" + filtro(texto), Long.class);
    Busqueda.patron(texto).ifPresent(p -> consulta.setParameter("patron", p));
    return consulta.getSingleResult();
  }

  private static String filtro(String texto) {
    if (Busqueda.patron(texto).isEmpty()) {
      return "";
    }
    return " where lower(d.numDocumento) like :patron"
        + " or d.id in (select h.id from PersonaHumana h"
        + "   where lower(concat(h.nombre, ' ', h.apellido)) like :patron)"
        + " or d.id in (select j.id from PersonajeJuridica j"
        + "   where lower(j.razonSocial) like :patron)"
        + " or d.id in (select o.id from Donante o join o.mediosDeContacto m"
        + "   where lower(m.contacto) like :patron)";
  }

  public List<Donante> getDonantesInactivos(java.time.LocalDate limite) {
    return createQuery(
            "select d from Donante d where not exists "
                + "(select don from Donacion don where don.donante = d and "
                + "exists (select h from don.historialEstado h where h.fecha >= :limite))",
            Donante.class)
        .setParameter("limite", limite.atStartOfDay())
        .getResultList();
  }
}
