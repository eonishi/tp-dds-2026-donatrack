package ar.edu.utn.donatrack.propuestas.matchmaking;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
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
  protected List<MatchCandidato> ordenarCandidatos(
      Donacion donacion,
      Collection<EntidadBeneficiaria> entidadesBeneficiarias,
      LocalDate fechaReferencia) {
    List<MatchCandidato> candidatos = new ArrayList<>();
    for (EntidadBeneficiaria entidad : entidadesBeneficiarias) {
      int puntaje = entidad.calcularPuntajeCompatibilidad(donacion);
      if (puntaje > 0) {
        candidatos.add(new MatchCandidato(entidad, puntaje));
      }
    }

    candidatos.sort(Comparator.comparingInt(MatchCandidato::getPuntaje).reversed());
    return candidatos;
  }
}
