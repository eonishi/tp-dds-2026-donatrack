package ar.edu.utn.donatrack.api.dto;

import ar.edu.utn.donatrack.bienes.Categoria;
import ar.edu.utn.donatrack.bienes.EstadoUso;
import ar.edu.utn.donatrack.bienes.UnidadMedida;
import ar.edu.utn.donatrack.donaciones.EstadoDonacion;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class DonacionDtos {
  private DonacionDtos() {}

  public record CargaDonacionRequest(
      @NotNull UUID donanteId, @NotEmpty List<@Valid BienRequest> bienes) {}

  public record DonacionUpdateRequest(@NotEmpty List<@Valid BienRequest> bienes) {}

  public record BienRequest(
      @NotBlank String descripcion,
      String foto,
      @Positive double cantidad,
      @NotNull UnidadMedida unidad,
      LocalDate fechaVencimiento,
      EstadoUso estadoDeUso,
      @NotBlank String subcategoria,
      @NotNull Categoria categoria) {}

  public record BienResponse(
      String descripcion,
      String foto,
      double cantidad,
      UnidadMedida unidad,
      LocalDate fechaVencimiento,
      EstadoUso estadoDeUso,
      String subcategoria,
      Categoria categoria) {}

  public record DonacionResponse(
      UUID id,
      UUID donanteId,
      Long entidadBeneficiariaId,
      EstadoDonacion estado,
      List<BienResponse> bienes,
      List<String> fotos) {}

  public record TransicionEstadoRequest(
      @NotNull EstadoDonacion estado, String justificacion, @JsonAlias("patente") String idCamion) {
    @AssertTrue(message = "no admite ASIGNACION_REALIZADA")
    public boolean isEstadoPermitido() {
      return estado != EstadoDonacion.ASIGNACION_REALIZADA;
    }
  }

  public record SeleccionEntidadRequest(
      @NotNull @JsonAlias({"beneficiarioId", "entidadId"}) Long entidadBeneficiariaId) {}

  public record FotoRequest(@NotBlank @JsonAlias({"referencia", "foto"}) String url) {}

  public record FotoResponse(UUID donacionId, String url) {}

  public record HistorialEstadoResponse(
      LocalDateTime fecha, EstadoDonacion estadoAlcanzado, String justificacion) {}
}
