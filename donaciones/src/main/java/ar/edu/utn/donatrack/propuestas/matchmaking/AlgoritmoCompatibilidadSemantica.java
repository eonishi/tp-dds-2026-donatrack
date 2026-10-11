package ar.edu.utn.donatrack.propuestas.matchmaking;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.beneficiarios.Necesidad;
import ar.edu.utn.donatrack.donaciones.Donacion;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class AlgoritmoCompatibilidadSemantica extends AlgoritmoMatchmaking {
  @Override
  public String getNombre() {
    return "compatibilidad-semantica";
  }

  @Override
  protected List<MatchCandidato> candidatosOrdenados(
      Donacion donacion,
      Collection<EntidadBeneficiaria> entidadesBeneficiarias,
      LocalDate fechaReferencia) {
    List<MatchCandidato> candidatos = new ArrayList<>();
    for (EntidadBeneficiaria entidad : entidadesBeneficiarias) {
      int puntaje = calcularPuntaje(donacion, entidad);
      if (puntaje > 0) {
        candidatos.add(new MatchCandidato(entidad, puntaje));
      }
    }

    candidatos.sort(Comparator.comparingInt(MatchCandidato::getPuntaje).reversed());
    return candidatos;
  }

  private int calcularPuntaje(Donacion donacion, EntidadBeneficiaria entidad) {
    return necesidades(entidad).stream()
        .mapToInt(necesidad -> puntajeNecesidad(donacion, necesidad))
        .max()
        .orElse(0);
  }

  private List<Necesidad> necesidades(EntidadBeneficiaria entidad) {
    List<Necesidad> necesidades = new ArrayList<>(entidad.getNecesidadesExtraordinarias());
    necesidades.addAll(entidad.getNecesidadesRecurrentes());
    return necesidades;
  }

  private int puntajeNecesidad(Donacion donacion, Necesidad necesidad) {
    int puntaje = 0;
    if (!donacion.getSubcategoria().equals(necesidad.getSubcategoria())) {
      return 0;
    }

    puntaje += 50;

    if (donacion.getUnidad().equals(necesidad.getUnidad())) {
      puntaje += 15;
    }
    if (Double.compare(donacion.getCantidadTotal(), necesidad.getCantidadObjetivo()) >= 0) {
      puntaje += 10;
    }

    return puntaje;
  }
}
