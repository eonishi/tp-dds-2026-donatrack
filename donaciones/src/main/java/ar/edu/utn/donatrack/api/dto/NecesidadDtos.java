package ar.edu.utn.donatrack.api.dto;

import ar.edu.utn.donatrack.beneficiarios.TipoPeriodo;
import ar.edu.utn.donatrack.bienes.Categoria;
import ar.edu.utn.donatrack.bienes.UnidadMedida;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public final class NecesidadDtos {
  private NecesidadDtos() {}

  public record NecesidadRequest(
      @NotNull @JsonAlias({"entidadId", "beneficiarioId"}) UUID entidadBeneficiariaId,
      @NotBlank String tipo,
      TipoPeriodo periodo,
      @NotBlank String descripcion,
      @Positive double cantidadObjetivo,
      @NotNull UnidadMedida unidad,
      @NotBlank String subcategoria,
      @NotNull Categoria categoria) {}

  public record NecesidadResponse(
      UUID id,
      UUID entidadBeneficiariaId,
      String tipo,
      TipoPeriodo periodo,
      String descripcion,
      double cantidadObjetivo,
      double cantidadRecibida,
      boolean satisfecha,
      UnidadMedida unidad,
      String subcategoria,
      Categoria categoria) {}
}
