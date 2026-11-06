package ar.edu.utn.donatrack.logistica.controllers;

import ar.edu.utn.donatrack.logistica.Repartidor;
import ar.edu.utn.donatrack.logistica.RepartidorRepository;
import ar.edu.utn.donatrack.logistica.Ruta;
import ar.edu.utn.donatrack.logistica.RutaRepository;
import ar.edu.utn.donatrack.logistica.security.LogisticaSecurity;
import io.javalin.http.Context;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acceso de los repartidores a su aplicación y su asignación a las rutas.
 *
 * <p>El login devuelve el token en el body y no en una cookie: lo consume la aplicación del
 * repartidor, que lo manda como {@code Authorization: Bearer}. Así tampoco pisa la cookie de sesión
 * de la web de donaciones, que en el mismo host se comparte entre puertos.
 */
public class RepartidorController {

  private final RepartidorRepository repartidorRepo;
  private final RutaRepository rutaRepo;
  private final Clock clock;

  public RepartidorController(
      RepartidorRepository repartidorRepo, RutaRepository rutaRepo, Clock clock) {
    this.repartidorRepo = repartidorRepo;
    this.rutaRepo = rutaRepo;
    this.clock = clock;
  }

  // POST /logistica/login
  public void login(Context ctx) {
    LoginDto dto = ctx.bodyAsClass(LoginDto.class);
    Optional<Repartidor> repartidor =
        repartidorRepo.buscarPorEmail(dto.email).filter(r -> r.tieneContrasenia(dto.password));
    if (repartidor.isEmpty()) {
      ctx.status(401).result("Credenciales inválidas");
      return;
    }
    String token = LogisticaSecurity.emitirToken(repartidor.get(), clock.instant());
    ctx.json(new LoginResponseDto(token, repartidor.get().getId(), repartidor.get().getNombre()));
  }

  // POST /logistica/logout: revoca el token que manda la aplicación del repartidor.
  public void logout(Context ctx) {
    LogisticaSecurity.cerrarSesion(ctx);
    ctx.status(204);
  }

  // GET /logistica/repartidores
  public void listar(Context ctx) {
    List<RepartidorDto> repartidores =
        repartidorRepo.listarTodos().stream()
            .map(r -> new RepartidorDto(r.getId(), r.getNombre(), r.getEmail()))
            .toList();
    ctx.json(repartidores);
  }

  // POST /logistica/rutas/{id}/repartidores
  public void asignarRuta(Context ctx) {
    UUID idRuta;
    UUID idRepartidor;
    try {
      idRuta = UUID.fromString(ctx.pathParam("id"));
      idRepartidor = UUID.fromString(ctx.bodyAsClass(AsignacionDto.class).repartidorId);
    } catch (IllegalArgumentException | NullPointerException e) {
      ctx.status(400).result("Formato de UUID inválido");
      return;
    }

    Optional<Ruta> ruta = rutaRepo.buscarPorId(idRuta);
    if (ruta.isEmpty()) {
      ctx.status(404).result("Ruta no encontrada");
      return;
    }
    Optional<Repartidor> repartidor = repartidorRepo.buscarPorId(idRepartidor);
    if (repartidor.isEmpty()) {
      ctx.status(400).result("Repartidor inexistente");
      return;
    }

    ruta.get().asignarRepartidor(repartidor.get());
    ctx.status(200).json(ruta.get());
  }

  public static class LoginDto {
    public String email;
    public String password;
  }

  public static class AsignacionDto {
    public String repartidorId;
  }

  public record LoginResponseDto(String token, UUID repartidorId, String nombre) {}

  public record RepartidorDto(UUID id, String nombre, String email) {}
}
