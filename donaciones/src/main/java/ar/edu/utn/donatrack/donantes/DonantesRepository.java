package ar.edu.utn.donatrack.donantes;

import ar.edu.utn.donatrack.persistencia.JpaRepository;
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
public class DonantesRepository implements JpaRepository {

  /**
   * @return una instantánea inmutable de todos los donantes almacenados.
   */
  public Set<Donante> getDonantes() {
    return Set.copyOf(createQuery("select d from Donante d", Donante.class).getResultList());
  }

  /**
   * Persiste todos los donantes del lote dentro de una única transacción.
   *
   * @param nuevos los donantes a registrar
   */
  public void addMany(List<Donante> nuevos) {
    inTransaction(() -> nuevos.forEach(this::persist));
  }

  /**
   * Inserta los donantes entrantes que todavía no están almacenados y fusiona los datos de dominio
   * de los que ya existen, emparejando por correo electrónico.
   *
   * @param entrantes los donantes a insertar o actualizar
   */
  public void upsert(List<Donante> entrantes) {
    if (entrantes == null || entrantes.isEmpty()) {
      return;
    }
    inTransaction(
        () -> {
          // La tabla completa de donantes se carga a propósito: la implementación en memoria
          // anterior emparejaba cada donante entrante contra todos los donantes almacenados por
          // correo electrónico, y reproducir exactamente esa semántica de emparejamiento requiere
          // la misma pasada en memoria. El costo por llamada es aceptable para este flujo de
          // importación, que corre según la agenda del operador.
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
   * @param donante el donante a guardar
   * @return la instancia administrada resultante
   */
  public Donante save(Donante donante) {
    return inTransaction(
        () -> {
          if (entityManager().contains(donante)) {
            return donante;
          }
          return merge(donante);
        });
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
   * @param id identidad del donante
   */
  public void deleteById(UUID id) {
    inTransaction(
        () -> {
          Donante donante = find(Donante.class, id);
          if (donante != null) {
            remove(donante);
          }
        });
  }
}
