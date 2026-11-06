package ar.edu.utn.donatrack.api.controller;

import static io.javalin.apibuilder.ApiBuilder.post;

import ar.edu.utn.donatrack.api.error.ApiException;
import ar.edu.utn.donatrack.beneficiarios.BeneficiariosRepository;
import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiariaId;
import ar.edu.utn.donatrack.donantes.DonantesRepository;
import ar.edu.utn.donatrack.security.AuthService;
import ar.edu.utn.donatrack.security.Role;
import ar.edu.utn.donatrack.security.Usuario;
import io.javalin.http.Context;
import java.util.Set;
import java.util.UUID;

/**
 * Alta de usuarios por parte de la persona administradora (solo ADMIN, ver SecurityRules).
 *
 * <p>Le da acceso a una persona donante o a una entidad beneficiaria que ya está registrada, y deja
 * el usuario vinculado a ese registro para que vea solo sus donaciones.
 */
public final class UsuarioController {
  private final AuthService authService;
  private final DonantesRepository donantes;
  private final BeneficiariosRepository entidades;

  public UsuarioController(
      AuthService authService, DonantesRepository donantes, BeneficiariosRepository entidades) {
    this.authService = authService;
    this.donantes = donantes;
    this.entidades = entidades;
  }

  public void routes() {
    post("/usuarios", this::crear);
  }

  private void crear(Context ctx) {
    UsuarioRequest request = ctx.bodyAsClass(UsuarioRequest.class);
    if (request.email == null || request.email.isBlank()) {
      throw ApiException.badRequest("El email es obligatorio.");
    }
    if (request.password == null || request.password.length() < 8) {
      throw ApiException.badRequest("La contraseña debe tener al menos 8 caracteres.");
    }
    if (request.rol == null) {
      throw ApiException.badRequest("El rol es obligatorio (DONANTE o BENEFICIARIO).");
    }

    UUID donanteId = null;
    Long entidadId = null;
    switch (request.rol) {
      case DONANTE -> {
        donanteId = request.donanteId;
        if (donanteId == null || donantes.findById(donanteId).isEmpty()) {
          throw ApiException.badRequest("El usuario donante necesita un donanteId existente.");
        }
      }
      case BENEFICIARIO -> {
        entidadId = request.entidadId;
        if (entidadId == null
            || entidades.findById(new EntidadBeneficiariaId(entidadId)).isEmpty()) {
          throw ApiException.badRequest("El usuario beneficiario necesita un entidadId existente.");
        }
      }
      default ->
          throw ApiException.badRequest("Sólo se dan de alta usuarios DONANTE o BENEFICIARIO.");
    }

    try {
      Usuario usuario =
          authService.registrar(
              request.email, request.password, Set.of(request.rol), donanteId, entidadId);
      ctx.status(201)
          .json(
              new UsuarioResponse(
                  usuario.getId(),
                  usuario.getEmail(),
                  request.rol,
                  usuario.getDonanteId().orElse(null),
                  usuario.getEntidadId().orElse(null)));
    } catch (IllegalArgumentException e) {
      throw ApiException.conflict(e.getMessage());
    }
  }

  public static class UsuarioRequest {
    public String email;
    public String password;
    public Role rol;
    public UUID donanteId;
    public Long entidadId;
  }

  public record UsuarioResponse(UUID id, String email, Role rol, UUID donanteId, Long entidadId) {}
}
