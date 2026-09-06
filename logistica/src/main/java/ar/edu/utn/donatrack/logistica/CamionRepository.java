package ar.edu.utn.donatrack.logistica;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio en memoria para la gestión de camiones.
 *
 * <p>Almacena instancias de {@link Camion} en un {@link HashSet}, utilizando la identidad basada en
 * patente para la deduplicación. Sigue el patrón de {@code BeneficiariosRepository}.
 */
public class CamionRepository {

  private final HashSet<Camion> camiones = new HashSet<>();

  /**
   * Agrega un camión al repositorio.
   *
   * <p>Si ya existe un camión con la misma patente (según {@link Camion#equals}), el camión
   * existente es reemplazado (semántica de {@link HashSet#add}).
   *
   * @param camion el camión a agregar (no nulo)
   */
  public void agregar(Camion camion) {
    this.camiones.add(camion);
  }

  /**
   * @return copia inmodificable de todos los camiones almacenados
   */
  public List<Camion> listarTodos() {
    return Collections.unmodifiableList(new ArrayList<>(camiones));
  }

  /**
   * Busca un camión por su patente.
   *
   * @param patente la patente a buscar (no nula)
   * @return un {@link Optional} con el camión si existe, o {@link Optional#empty()} si no
   */
  public Optional<Camion> buscarPorPatente(String patente) {
    return camiones.stream().filter(c -> c.getPatente().equals(patente)).findFirst();
  }
}
