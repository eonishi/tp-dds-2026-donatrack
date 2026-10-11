package ar.edu.utn.donatrack.donaciones;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public abstract class Algoritmo {
  /** Hasta diez entidades beneficiarias por algoritmo (enunciado-2.md:34). */
  static final int MAXIMO_CANDIDATOS = 10;

  public abstract String getNombre();

  /** Lista ordenada de candidatos de la donación, con el tope ya aplicado. */
  public final List<MatchCandidato> rankear(
      Donacion donacion,
      Collection<EntidadBeneficiaria> entidadesBeneficiarias,
      LocalDate fechaReferencia) {
    return candidatosOrdenados(donacion, entidadesBeneficiarias, fechaReferencia).stream()
        .limit(MAXIMO_CANDIDATOS)
        .toList();
  }

  /** Candidatos ordenados por puntaje, antes del tope. Lista vacía = sin propuestas. */
  protected abstract List<MatchCandidato> candidatosOrdenados(
      Donacion donacion,
      Collection<EntidadBeneficiaria> entidadesBeneficiarias,
      LocalDate fechaReferencia);
}
