package ar.edu.utn.donatrack.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public final class EntidadDtos {
  private EntidadDtos() {}

  public record EntidadRequest(
      @NotBlank String razonSocial,
      @NotBlank String direccion,
      @NotBlank String telefono,
      List<@Email String> correos) {}

  public record EntidadResponse(
      long id,
      String razonSocial,
      String direccion,
      String telefono,
      List<String> correos,
      List<UUID> necesidades,
      int recepciones) {}
}
