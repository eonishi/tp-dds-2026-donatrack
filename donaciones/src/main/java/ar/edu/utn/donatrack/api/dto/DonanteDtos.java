package ar.edu.utn.donatrack.api.dto;

import ar.edu.utn.donatrack.donantes.Genero;
import ar.edu.utn.donatrack.donantes.TipoDocumento;
import ar.edu.utn.donatrack.donantes.TipoOrganizacion;
import ar.edu.utn.donatrack.utilidades.TipoPersona;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public final class DonanteDtos {
  private DonanteDtos() {}

  public record DonanteRequest(
      @NotNull TipoPersona tipoPersona,
      @NotBlank String numeroDocumento,
      @NotNull TipoDocumento tipoDocumento,
      @NotEmpty List<@Valid MedioContactoRequest> mediosDeContacto,
      Integer contactoPredeterminado,
      String nombre,
      String apellido,
      @Min(0) Integer edad,
      Genero genero,
      String direccion,
      String razonSocial,
      TipoOrganizacion tipoOrganizacion,
      String rubro,
      List<@Valid RepresentanteRequest> representantes) {}

  public record MedioContactoRequest(@NotBlank String tipo, @NotBlank String contacto) {}

  public record RepresentanteRequest(@NotBlank String nombre, @NotBlank String apellido) {}

  public record MedioContactoResponse(String tipo, String contacto, boolean predeterminado) {}

  public record DonanteResponse(
      UUID id,
      TipoPersona tipoPersona,
      String numeroDocumento,
      TipoDocumento tipoDocumento,
      List<MedioContactoResponse> mediosDeContacto,
      String nombre,
      String apellido,
      Integer edad,
      Genero genero,
      String direccion,
      String razonSocial,
      TipoOrganizacion tipoOrganizacion,
      String rubro,
      List<RepresentanteRequest> representantes) {}
}
