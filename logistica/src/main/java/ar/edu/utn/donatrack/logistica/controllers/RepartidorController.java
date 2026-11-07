package ar.edu.utn.donatrack.logistica.controllers;

import ar.edu.utn.donatrack.logistica.Repartidor;
import ar.edu.utn.donatrack.logistica.RepartidorRepository;
import ar.edu.utn.donatrack.logistica.security.LogisticaSecurity;
import io.javalin.http.Context;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

/**
 * Sesión del repartidor en su aplicación. Queda en HTTP: el resto de las operaciones (rutas,
 * posiciones, asignaciones) van por GraphQL.
 *
 * <p>El login devuelve el token en el body y no en una cookie: lo consume la aplicación del
 * repartidor, que lo manda como {@code Authorization: Bearer}. Así tampoco pisa la cookie de sesión
 * de la web de donaciones, que en el mismo host se comparte entre puertos.
 */
public class RepartidorController {

  private final RepartidorRepository repartidorRepo;
  private final Clock clock;

  public RepartidorController(RepartidorRepository repartidorRepo, Clock clock) {
    this.repartidorRepo = repartidorRepo;
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

  public static class LoginDto {
    public String email;
    public String password;
  }

  public record LoginResponseDto(String token, UUID repartidorId, String nombre) {}
}
