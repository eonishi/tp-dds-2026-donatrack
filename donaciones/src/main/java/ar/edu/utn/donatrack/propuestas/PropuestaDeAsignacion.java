package ar.edu.utn.donatrack.propuestas;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.propuestas.matchmaking.MatchCandidato;
import java.util.List;
import java.util.Objects;

public class PropuestaDeAsignacion {
  private final Donacion donacion;
  private final List<MatchCandidato> candidatos;
  private boolean pendiente = true;

  public PropuestaDeAsignacion(Donacion donacion, List<List<MatchCandidato>> listasDeCandidatos) {
    this.donacion = Objects.requireNonNull(donacion, "donacion no puede ser null");
    this.candidatos =
        intersectarCandidatos(
            Objects.requireNonNull(listasDeCandidatos, "listasDeCandidatos no puede ser null"));
  }

  public void confirmar(EntidadBeneficiaria elegida) {
    if (!pendiente) {
      return; // no-op on double-confirm
    }

    if (!incluye(elegida)) {
      throw new EntidadFueraDeRankingException();
    }

    donacion.confirmarEntidadBeneficiaria(elegida);
    this.pendiente = false;
  }

  private boolean incluye(EntidadBeneficiaria entidadElegida) {
    return candidatos.stream().anyMatch(c -> c.coincideBeneficiaria(entidadElegida));
  }

  private List<MatchCandidato> intersectarCandidatos(
      List<List<MatchCandidato>> listasDeCandidatos) {
    List<EntidadBeneficiaria> beneficiariosCoincidentes =
        getBeneficiariosCoincidentes(listasDeCandidatos);

    if (!beneficiariosCoincidentes.isEmpty()) {
      return construirCandidatosConPromedio(listasDeCandidatos, beneficiariosCoincidentes);
    }

    // Sin entidades coincidentes se expone una lista plana: todas las entidades disponibles, una
    // sola vez cada una, con su puntaje promediado sobre todas las listas.
    return construirCandidatosConPromedio(
        listasDeCandidatos, getBeneficiariosDisponibles(listasDeCandidatos));
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
    return candidatos;
  }

  public boolean isPendiente() {
    return pendiente;
  }
}
