package ar.edu.utn.donatrack.logistica.security;

import ar.edu.utn.donatrack.logistica.Repartidor;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Autenticación y autorización del microservicio de logística.
 *
 * <p>Acepta dos tipos de token firmados con el mismo secreto: los de las personas administradoras,
 * emitidos por donaciones (cookie {@code access_token}), y los de los repartidores, emitidos por
 * este servicio en {@code POST /logistica/login} y enviados por su aplicación como {@code
 * Authorization: Bearer}. Donaciones y el planificador se autentican con la clave interna.
 */
public final class LogisticaSecurity {
  /** Atributo del request donde queda el id del repartidor autenticado, si lo hay. */
  public static final String ATRIBUTO_REPARTIDOR = "repartidorId";

  private static final String COOKIE_NAME = "access_token";
  private static final Algorithm ALGORITHM =
      Algorithm.HMAC256(
          System.getenv().getOrDefault("DONATRACK_JWT_SECRET", "donatrack-dev-secret-change-me"));
  private static final JWTVerifier VERIFIER =
      JWT.require(ALGORITHM).withClaimPresence("exp").build();
  // Clave interna entre servicios. Sin configurar queda desactivada.
  private static final String API_KEY = System.getenv().getOrDefault("DONATRACK_API_KEY", "");
  // Una jornada de reparto completa: el repartidor no debería tener que volver a loguearse a
  // mitad de la ruta.
  private static final Duration DURACION_TOKEN_REPARTIDOR = Duration.ofHours(12);

  // Sesiones abiertas de repartidores, por id de sesión (claim "sid"). Un token de repartidor solo
  // vale mientras su sesión esté acá: el logout la borra y el token deja de servir aunque no haya
  // expirado. Viven en memoria, igual que las sesiones de donaciones.
  private static final Map<String, SesionRepartidor> SESIONES = new ConcurrentHashMap<>();

  private record SesionRepartidor(UUID repartidorId, Instant expira) {}

  private LogisticaSecurity() {}

  /**
   * Filtro de acceso de todas las rutas {@code /logistica/...}. Solo verifica que haya credenciales
   * válidas: los permisos de cada operación GraphQL los decide su resolver. Si el que llama es un
   * repartidor, deja su id en {@link #ATRIBUTO_REPARTIDOR}.
   */
  public static void autorizar(Context ctx) {
    String path = ctx.path();
    // Las consultas previas de CORS (OPTIONS) las manda el navegador sin credenciales.
    if ("OPTIONS".equals(ctx.method().toString())) {
      return;
    }
    if (!path.startsWith("/logistica/")
        || "/logistica/login".equals(path)
        || "/logistica/logout".equals(path)) {
      return;
    }
    if ("/logistica/planificacion/callback".equals(path) && isServiceRequest(ctx)) {
      return;
    }
    // Donaciones entra con la clave interna; en GraphQL solo puede consultar posiciones (ver
    // Acceso).
    if ("/logistica/graphql".equals(path) && isServiceRequest(ctx)) {
      return;
    }
    if (path.startsWith("/logistica/planificacion")) {
      enforce(ctx);
    } else {
      enforce(ctx, Role.REPARTIDOR);
    }
    repartidorActual(ctx).ifPresent(id -> ctx.attribute(ATRIBUTO_REPARTIDOR, id));
  }

  /** Si quien llama es una persona administradora (token emitido por donaciones). */
  public static boolean esAdministrador(Context ctx) {
    return hasRole(ctx, Role.ADMIN);
  }

  /** Exige un usuario ADMIN o con alguno de los roles indicados. Sin roles, solo ADMIN. */
  public static void enforce(Context ctx, Role... requiredRoles) {
    if (hasRole(ctx, Role.ADMIN)) {
      return;
    }
    for (Role role : requiredRoles) {
      if (hasRole(ctx, role)) {
        return;
      }
    }
    if (hasToken(ctx)) {
      throw new ForbiddenResponse("No tiene permisos para acceder a esta operación.");
    }
    throw new UnauthorizedResponse("Debe iniciar sesión para acceder a esta operación.");
  }

  public static boolean isServiceRequest(Context ctx) {
    String header = ctx.header("Authorization");
    if (API_KEY.isBlank() || header == null || header.isBlank()) {
      return false;
    }
    return MessageDigest.isEqual(
        API_KEY.getBytes(StandardCharsets.UTF_8), header.getBytes(StandardCharsets.UTF_8));
  }

  /** Emite el token que la aplicación del repartidor manda en cada request. */
  public static String emitirToken(Repartidor repartidor, Instant ahora) {
    Instant expira = ahora.plus(DURACION_TOKEN_REPARTIDOR);
    String sesion = UUID.randomUUID().toString();
    SESIONES.put(sesion, new SesionRepartidor(repartidor.getId(), expira));
    return JWT.create()
        .withSubject(repartidor.getId().toString())
        .withClaim("email", repartidor.getEmail())
        .withClaim("sid", sesion)
        .withClaim("roles", List.of(Role.REPARTIDOR.name()))
        .withExpiresAt(expira)
        .sign(ALGORITHM);
  }

  /** El id del repartidor autenticado. Vacío si no hay sesión o si quien llama es ADMIN. */
  public static Optional<UUID> repartidorActual(Context ctx) {
    if (hasRole(ctx, Role.ADMIN) || !hasRole(ctx, Role.REPARTIDOR)) {
      return Optional.empty();
    }
    try {
      return Optional.of(UUID.fromString(verifiedToken(ctx).getSubject()));
    } catch (IllegalArgumentException | NullPointerException e) {
      return Optional.empty();
    }
  }

  /**
   * Cierra la sesión del repartidor que manda el token: desde ese momento el token no sirve más.
   * Sin token o con uno que no es de repartidor no hace nada.
   */
  public static void cerrarSesion(Context ctx) {
    DecodedJWT jwt = verifiedToken(ctx);
    if (jwt != null && jwt.getClaim("sid").asString() != null) {
      SESIONES.remove(jwt.getClaim("sid").asString());
    }
  }

  private static boolean hasRole(Context ctx, Role required) {
    DecodedJWT jwt = verifiedToken(ctx);
    return jwt != null && roles(jwt).contains(required.name());
  }

  private static boolean hasToken(Context ctx) {
    return verifiedToken(ctx) != null;
  }

  // Un claim de roles mal formado cuenta como sin roles (403), no como error.
  private static List<String> roles(DecodedJWT jwt) {
    try {
      List<String> roles = jwt.getClaim("roles").asList(String.class);
      return roles == null ? List.of() : roles;
    } catch (JWTDecodeException e) {
      return List.of();
    }
  }

  // Primero el Bearer (aplicación del repartidor) y después la cookie (administración en la web).
  private static DecodedJWT verifiedToken(Context ctx) {
    String token = null;
    String authorization = ctx.header("Authorization");
    if (authorization != null && authorization.startsWith("Bearer ")) {
      token = authorization.substring("Bearer ".length());
    }
    if (token == null || token.isBlank()) {
      token = ctx.cookie(COOKIE_NAME);
    }
    if (token == null || token.isBlank()) {
      return null;
    }
    try {
      DecodedJWT jwt = VERIFIER.verify(token);
      if (esTokenDeRepartidor(jwt) && !sesionVigente(jwt)) {
        return null;
      }
      return jwt;
    } catch (JWTVerificationException e) {
      return null;
    }
  }

  // Los tokens de la administración los emite donaciones, que controla sus propias sesiones.
  private static boolean esTokenDeRepartidor(DecodedJWT jwt) {
    List<String> roles = roles(jwt);
    return roles.contains(Role.REPARTIDOR.name()) && !roles.contains(Role.ADMIN.name());
  }

  // La sesión tiene que existir, no haber expirado y ser del mismo repartidor que dice el token.
  private static boolean sesionVigente(DecodedJWT jwt) {
    String sid = jwt.getClaim("sid").asString();
    SesionRepartidor sesion = sid == null ? null : SESIONES.get(sid);
    if (sesion == null) {
      return false;
    }
    if (sesion.expira().isBefore(Instant.now())) {
      SESIONES.remove(sid);
      return false;
    }
    return sesion.repartidorId().toString().equals(jwt.getSubject());
  }
}
