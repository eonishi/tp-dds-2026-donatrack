package ar.edu.utn.donatrack.logistica;

import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de gestión de rutas respaldado por JPA.
 *
 * <p>Sigue el patrón de {@link CamionRepository}: las escrituras requieren una transacción activa
 * del llamador (el caso de uso), el repositorio nunca abre ni confirma una transacción por su
 * cuenta; las lecturas se resuelven con consultas simples, sin transacción explícita.
 */
public class RutaRepository implements WithSimplePersistenceUnit {

  /**
   * Agrega una ruta al repositorio.
   *
   * <p>Requiere una transacción activa del llamador (el caso de uso); nunca abre ni confirma una
   * transacción por su cuenta.
   *
   * @param ruta la ruta a agregar (no nula)
   */
  public void agregar(Ruta ruta) {
    persist(ruta);
  }

  /**
   * Busca una ruta por su identificador único.
   *
   * @param id el identificador de la ruta (no nulo)
   * @return un {@link Optional} con la ruta si existe, o {@link Optional#empty()} si no
   */
  public Optional<Ruta> buscarPorId(UUID id) {
    return Optional.ofNullable(find(Ruta.class, id));
  }

  /**
   * Busca la ruta activa (enCurso = true) asociada a un camión.
   *
   * @param camion el camión a buscar (no nulo)
   * @return un {@link Optional} con la ruta activa si existe, o {@link Optional#empty()} si no
   */
  public Optional<Ruta> buscarPorCamion(Camion camion) {
    return createQuery(
            "select r from Ruta r where r.camion = :camion and r.enCurso = true", Ruta.class)
        .setParameter("camion", camion)
        .getResultList()
        .stream()
        .findFirst();
  }

  /**
   * @return copia de la lista de todas las rutas almacenadas
   */
  public List<Ruta> listarTodas() {
    return new ArrayList<>(createQuery("select r from Ruta r", Ruta.class).getResultList());
  }

  /**
   * Elimina una ruta por su identificador único. Si no existe, no hace nada.
   *
   * <p>Requiere una transacción activa del llamador (el caso de uso); nunca abre ni confirma una
   * transacción por su cuenta.
   *
   * @param id el identificador de la ruta a eliminar
   */
  public void eliminar(UUID id) {
    buscarPorId(id).ifPresent(this::remove);
  }
}
