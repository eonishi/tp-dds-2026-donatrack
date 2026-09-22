package ar.edu.utn.donatrack.donaciones;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import java.time.LocalDate;
import java.util.Collection;

public interface AlgoritmoMatchmaking {
  String getNombre();

  RankingMatchmaking rankear(
      Donacion donacion,
      Collection<EntidadBeneficiaria> entidadesBeneficiarias,
      LocalDate fechaReferencia);
}
