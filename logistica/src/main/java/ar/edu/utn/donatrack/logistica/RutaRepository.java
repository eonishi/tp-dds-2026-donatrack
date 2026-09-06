package ar.edu.utn.donatrack.logistica;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio en memoria para la gestión de rutas.
 *
 * <p>Almacena instancias de {@link Ruta} en un {@link HashMap} indexadas por su {@link UUID}. Sigue
 * el patrón de {@link CamionRepository}.
 */
public class RutaRepository {

  private final HashMap<UUID, Ruta> rutas = new HashMap<>();

  /**
   * Agrega una ruta al repositorio.
   *
   * @param ruta la ruta a agregar (no nula)
   */
  public void agregar(Ruta ruta) {
    this.rutas.put(ruta.getId(), ruta);
  }

  /**
   * Busca una ruta por su identificador único.
   *
   * @param id el identificador de la ruta (no nulo)
   * @return un {@link Optional} con la ruta si existe, o {@link Optional#empty()} si no
   */
  public Optional<Ruta> buscarPorId(UUID id) {
    return Optional.ofNullable(rutas.get(id));
  }

  /**
   * Busca la ruta activa (enCurso = true) asociada a un camión.
   *
   * @param camion el camión a buscar (no nulo)
   * @return un {@link Optional} con la ruta activa si existe, o {@link Optional#empty()} si no
   */
  public Optional<Ruta> buscarPorCamion(Camion camion) {
    return rutas.values().stream()
        .filter(r -> r.getCamion().equals(camion) && r.isEnCurso())
        .findFirst();
  }

  /**
   * @return copia de la lista de todas las rutas almacenadas
   */
  public List<Ruta> listarTodas() {
    return new ArrayList<>(rutas.values());
  }
}
