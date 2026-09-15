package ar.edu.utn.donatrack.api.dto;

import java.util.List;
import java.util.UUID;

public final class MatchmakingDtos {
  private MatchmakingDtos() {}

  public record CandidatoResponse(UUID entidadBeneficiariaId, String razonSocial, int puntaje) {}

  public record RankingResponse(String algoritmo, List<CandidatoResponse> candidatos) {}

  public record PropuestaResponse(
      UUID donacionId, boolean pendiente, List<RankingResponse> rankings) {}

}
