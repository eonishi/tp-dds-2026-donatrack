package ar.edu.utn.donatrack.propuestas;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.propuestas.matchmaking.MatchCandidato;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;

@Entity
public class PropuestaDeAsignacion {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @OneToOne
  @JoinColumn(name = "donacion_id", nullable = false, unique = true)
  private Donacion donacion;

  @ElementCollection
  @CollectionTable(name = "propuesta_candidatos", joinColumns = @JoinColumn(name = "propuesta_id"))
  @OrderColumn(name = "orden")
  private List<MatchCandidato> candidatos = new ArrayList<>();

  @Column(nullable = false)
  private boolean pendiente = true;

  protected PropuestaDeAsignacion() {}

  public PropuestaDeAsignacion(Donacion donacion, List<List<MatchCandidato>> listasDeCandidatos) {
    this.donacion = Objects.requireNonNull(donacion, "donacion no puede ser null");
    this.candidatos =
        new ArrayList<>(
            intersectarCandidatos(
                Objects.requireNonNull(
                    listasDeCandidatos, "listasDeCandidatos no puede ser null")));
  }

  /**
   * Reemplaza la lista de candidatos de la propuesta y la vuelve a dejar pendiente.
   *
   * @param nuevosCandidatos nueva lista ordenada de candidatos (no nula)
   */
  public void reemplazarCandidatos(List<MatchCandidato> nuevosCandidatos) {
    Objects.requireNonNull(nuevosCandidatos, "nuevosCandidatos no puede ser null");
    this.candidatos.clear();
    this.candidatos.addAll(nuevosCandidatos);
    this.pendiente = true;
  }

  /**
   * Confirma la propuesta con la entidad elegida.
   *
   * <p>La propuesta vive en memoria y puede sobrevivir a la solicitud que la creó: la donación que
   * referencia queda separada del contexto de persistencia y su historial no se puede inicializar.
   * Por eso acá solo se valida la elegida y se marca la propuesta como confirmada; la transición de
   * la donación la aplica el llamador sobre la instancia administrada.
   *
   * @param elegida entidad beneficiaria elegida, que debe pertenecer a los candidatos
   * @throws EntidadFueraDeRankingException si la entidad no pertenece a los candidatos
   */
  public void confirmar(EntidadBeneficiaria elegida) {
    if (!pendiente) {
      return; // no-op on double-confirm
    }

    if (!incluye(elegida)) {
      throw new EntidadFueraDeRankingException();
    }

    this.pendiente = false;
  }

  private boolean incluye(EntidadBeneficiaria entidadElegida) {
    return candidatos.stream().anyMatch(c -> c.coincideBeneficiaria(entidadElegida));
  }

  /**
   * Arma la lista plana de candidatos que expone la propuesta.
   *
   * <p>Si todas las listas comparten entidades, expone la intersección; si no hay ninguna
   * coincidencia, expone la unión de las entidades disponibles, una sola vez cada una. En ambos
   * casos los candidatos quedan ordenados por puntaje, de mayor a menor.
   *
   * @param listasDeCandidatos candidatos propuestos por cada algoritmo
   * @return la intersección o la unión, ordenada por puntaje descendente
   */
  private List<MatchCandidato> intersectarCandidatos(
      List<List<MatchCandidato>> listasDeCandidatos) {
    List<EntidadBeneficiaria> beneficiariosCoincidentes =
        getBeneficiariosCoincidentes(listasDeCandidatos);

    // Sin entidades coincidentes se expone una lista plana: todas las entidades disponibles, una
    // sola vez cada una, con su puntaje promediado sobre todas las listas.
    List<EntidadBeneficiaria> beneficiarios =
        beneficiariosCoincidentes.isEmpty()
            ? getBeneficiariosDisponibles(listasDeCandidatos)
            : beneficiariosCoincidentes;

    return ordenarPorPuntaje(construirCandidatosConPromedio(listasDeCandidatos, beneficiarios));
  }

  /**
   * Ordena los candidatos por puntaje, de mayor a menor.
   *
   * <p>El orden es estable: los empates conservan el orden en el que se armó la lista.
   *
   * @param candidatos candidatos a ordenar
   * @return los candidatos ordenados por puntaje descendente
   */
  private List<MatchCandidato> ordenarPorPuntaje(List<MatchCandidato> candidatos) {
    return candidatos.stream()
        .sorted(Comparator.comparingInt(MatchCandidato::getPuntaje).reversed())
        .toList();
  }

  /**
   * Entidades beneficiarias de todas las listas, sin repetir, en orden de primera aparición.
   *
   * @param listasDeCandidatos candidatos propuestos por cada algoritmo
   * @return la unión de las entidades disponibles; vacía si no hay listas
   */
  private List<EntidadBeneficiaria> getBeneficiariosDisponibles(
      List<List<MatchCandidato>> listasDeCandidatos) {
    return listasDeCandidatos.stream()
        .flatMap(List::stream)
        .map(MatchCandidato::getEntidadBeneficiaria)
        .distinct()
        .toList();
  }

  /**
   * Entidades beneficiarias que aparecen en todas las listas, en el orden de la primera.
   *
   * @param listasDeCandidatos candidatos propuestos por cada algoritmo
   * @return las entidades coincidentes; vacía si ninguna aparece en todas las listas
   */
  private List<EntidadBeneficiaria> getBeneficiariosCoincidentes(
      List<List<MatchCandidato>> listasDeCandidatos) {
    if (listasDeCandidatos.isEmpty()) {
      return List.of();
    }

    return listasDeCandidatos.get(0).stream()
        .map(MatchCandidato::getEntidadBeneficiaria)
        .filter(entidad -> apareceEnTodasLasListas(listasDeCandidatos, entidad))
        .toList();
  }

  private boolean apareceEnTodasLasListas(
      List<List<MatchCandidato>> listasDeCandidatos, EntidadBeneficiaria entidad) {
    return listasDeCandidatos.stream()
        .allMatch(
            lista -> lista.stream().anyMatch(candidato -> candidato.coincideBeneficiaria(entidad)));
  }

  /**
   * Arma un candidato por entidad beneficiaria con el promedio de sus puntajes.
   *
   * <p>El promedio se calcula sobre todas las listas: una entidad ausente en una lista aporta 0,
   * así que el puntaje siempre se divide por la cantidad de listas.
   *
   * @param listasDeCandidatos candidatos propuestos por cada algoritmo
   * @param beneficiarios entidades a promediar, sin repetir
   * @return los candidatos con su puntaje promedio, truncado a entero
   */
  private List<MatchCandidato> construirCandidatosConPromedio(
      List<List<MatchCandidato>> listasDeCandidatos, List<EntidadBeneficiaria> beneficiarios) {
    return beneficiarios.stream()
        .map(entidad -> new MatchCandidato(entidad, puntajePromedio(listasDeCandidatos, entidad)))
        .toList();
  }

  private int puntajePromedio(
      List<List<MatchCandidato>> listasDeCandidatos, EntidadBeneficiaria entidad) {
    if (listasDeCandidatos.isEmpty()) {
      return 0;
    }

    int suma =
        listasDeCandidatos.stream()
            .flatMap(List::stream)
            .filter(candidato -> candidato.coincideBeneficiaria(entidad))
            .mapToInt(MatchCandidato::getPuntaje)
            .sum();
    return suma / listasDeCandidatos.size();
  }

  public Donacion getDonacion() {
    return donacion;
  }

  public List<MatchCandidato> getCandidatos() {
    return Collections.unmodifiableList(candidatos);
  }

  public boolean isPendiente() {
    return pendiente;
  }
}
