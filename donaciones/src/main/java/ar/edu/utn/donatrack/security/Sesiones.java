package ar.edu.utn.donatrack.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.javalin.http.Context;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Las sesiones de la web: lo único que sabe de la cookie, del token y de cuándo vence cada una.
 *
 * <p>La cookie lleva un JWT firmado porque logística lo valida con el mismo secreto para dejar
 * operar a la administración (la flota y las rutas). Pero acá el token no alcanza solo: además
 * tiene que estar su sesión abierta en memoria, así cerrar sesión lo invalida en el momento aunque
 * no haya vencido. Reiniciar el servicio solo obliga a volver a entrar.
 */
public final class Sesiones {
  static final String COOKIE = "access_token";
  static final Duration DURACION = Duration.ofMinutes(30);

  private static final Algorithm FIRMA =
      Algorithm.HMAC256(
          System.getenv().getOrDefault("DONATRACK_JWT_SECRET", "donatrack-dev-secret-change-me"));
  private static final JWTVerifier VERIFICADOR =
      JWT.require(FIRMA).withClaimPresence("exp").build();
  // Con HTTPS la cookie se marca Secure para que nunca viaje en texto plano.
  private static final boolean SOLO_HTTPS =
      "true".equalsIgnoreCase(System.getenv("DONATRACK_COOKIE_SECURE"));

  /** Sesión abierta: de quién es y hasta cuándo vale. La clave del mapa es el claim "sid". */
  private record Abierta(UUID usuarioId, Instant vence) {}

  private final Map<String, Abierta> abiertas = new ConcurrentHashMap<>();
  private final Clock reloj;

  public Sesiones() {
    this(Clock.systemUTC());
  }

  Sesiones(Clock reloj) {
    this.reloj = reloj;
  }

  /** Abre una sesión nueva para el usuario y le deja la cookie. */
  public void abrir(Context ctx, Usuario usuario) {
    Instant ahora = reloj.instant();
    Instant vence = ahora.plus(DURACION);
    // Se aprovecha cada ingreso para descartar las vencidas que nadie volvió a usar.
    abiertas.values().removeIf(sesion -> sesion.vence().isBefore(ahora));

    String sid = UUID.randomUUID().toString();
    abiertas.put(sid, new Abierta(usuario.getId(), vence));
    String token =
        JWT.create()
            .withSubject(usuario.getId().toString())
            .withClaim("email", usuario.getEmail())
            .withClaim("sid", sid)
            // Los roles van en el token solo para logística; acá se leen siempre de la base.
            .withClaim("roles", usuario.getRoles().stream().map(Enum::name).toList())
            .withExpiresAt(vence)
            .sign(FIRMA);
    ctx.header("Set-Cookie", cookie(token, DURACION.toSeconds()));
  }

  /** Cierra la sesión del request, si hay una, y borra la cookie. */
  public void cerrar(Context ctx) {
    decodificar(ctx).map(jwt -> jwt.getClaim("sid").asString()).ifPresent(abiertas::remove);
    ctx.header("Set-Cookie", cookie("", 0));
  }

  /**
   * De quién es la sesión del request: el token tiene que estar bien firmado, su sesión abierta y
   * sin vencer, y ser de la misma persona a la que se le abrió.
   */
  public Optional<UUID> usuarioId(Context ctx) {
    return decodificar(ctx)
        .flatMap(
            jwt -> {
              String sid = jwt.getClaim("sid").asString();
              Abierta sesion = sid == null ? null : abiertas.get(sid);
              if (sesion == null) {
                return Optional.empty();
              }
              if (sesion.vence().isBefore(reloj.instant())) {
                abiertas.remove(sid);
                return Optional.empty();
              }
              return sesion.usuarioId().toString().equals(jwt.getSubject())
                  ? Optional.of(sesion.usuarioId())
                  : Optional.empty();
            });
  }

  /** El token tal como vino, para reenviárselo a logística en nombre de la persona. */
  public static String token(Context ctx) {
    return Optional.ofNullable(ctx.cookie(COOKIE)).orElse("");
  }

  private static Optional<DecodedJWT> decodificar(Context ctx) {
    String token = token(ctx);
    if (token.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(VERIFICADOR.verify(token));
    } catch (JWTVerificationException e) {
      return Optional.empty();
    }
  }

  private static String cookie(String valor, long segundos) {
    return COOKIE
        + "="
        + valor
        + "; Path=/; Max-Age="
        + segundos
        + "; HttpOnly; SameSite=Lax"
        + (SOLO_HTTPS ? "; Secure" : "");
  }
}
