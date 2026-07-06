package ar.edu.utn.donatrack.donaciones;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.beneficiarios.Necesidad;
import ar.edu.utn.donatrack.bienes.Bien;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class AlgoritmoCompatibilidadSemantica implements AlgoritmoMatchmaking {
  @Override
  public String getNombre() {
    return "compatibilidad-semantica";
  }

  @Override
  public RankingMatchmaking rankear(
      Donacion donacion, Collection<EntidadBeneficiaria> entidadesBeneficiarias, LocalDate fechaReferencia) {
    List<MatchCandidato> candidatos = new ArrayList<>();
    for (EntidadBeneficiaria entidad : entidadesBeneficiarias) {
      int puntaje = calcularPuntaje(donacion, entidad, fechaReferencia);
      if (puntaje > 0) {
        candidatos.add(new MatchCandidato(entidad, puntaje));
      }
    }

    candidatos.sort(Comparator.comparingInt(MatchCandidato::getPuntaje).reversed());
    return new RankingMatchmaking(getNombre(), candidatos);
  }

  private int calcularPuntaje(
      Donacion donacion, EntidadBeneficiaria entidad, LocalDate fechaReferencia) {
    return necesidades(entidad).stream()
        .mapToInt(necesidad -> puntajeNecesidad(donacion, necesidad.getBienObjetivo(), fechaReferencia))
        .max()
        .orElse(0);
  }

  private List<Necesidad> necesidades(EntidadBeneficiaria entidad) {
    List<Necesidad> necesidades = new ArrayList<>(entidad.getNecesidadesExtraordinarias());
    necesidades.addAll(entidad.getNecesidadesRecurrentes());
    return necesidades;
  }

  private int puntajeNecesidad(Donacion donacion, Bien bienObjetivo, LocalDate fechaReferencia) {
    int puntaje = 0;
    if (!donacion.getSubcategoria().equals(bienObjetivo.getSubcategoria())) {
      return 0;
    }

    puntaje += 50;

    if (donacion.getUnidad().equals(bienObjetivo.getUnidad())) {
      puntaje += 15;
    }
    if (Double.compare(donacion.getCantidadTotal(), bienObjetivo.getCantidad()) >= 0) {
      puntaje += 10;
    }
    if (donacion.getBienes().get(0).getEstadoDeUso().equals(bienObjetivo.getEstadoDeUso())) {
      puntaje += 10;
    }
    if (donacion.esPerecedera() == bienObjetivo.esPerecedero()) {
      puntaje += 5;
    }
    if (!donacion.esPerecedera()) {
      return puntaje;
    }

    if (donacion.getFechaVencimiento() != null && donacion.getFechaVencimiento().isAfter(fechaReferencia)) {
      puntaje += 10;
    }

    if (bienObjetivo.getFechaVencimiento() != null
        && donacion.getFechaVencimiento() != null
        && !donacion.getFechaVencimiento().isBefore(bienObjetivo.getFechaVencimiento())) {
      puntaje += 10;
    }

    return puntaje;
  }
}
