package ar.edu.utn.donatrack.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.javalin.http.Context;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {
  private static final String COOKIE_NAME = "access_token";
  private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
  private static final Algorithm ALGORITHM =
      Algorithm.HMAC256(
          System.getenv().getOrDefault("DONATRACK_JWT_SECRET", "donatrack-dev-secret-change-me"));
  private static final JWTVerifier VERIFIER =
      JWT.require(ALGORITHM).withClaimPresence("exp").build();

  private final Map<String, Usuario> usuariosPorEmail = new ConcurrentHashMap<>();
  private final Map<String, SessionInfo> sesiones = new ConcurrentHashMap<>();

  public AuthService() {
    seedUsers();
  }

  public Optional<Usuario> authenticate(String email, String password) {
    if (email == null || password == null) {
      return Optional.empty();
    }
    Usuario usuario = usuariosPorEmail.get(normalizarEmail(email));
    if (usuario == null) {
      return Optional.empty();
    }
    if (!usuario.matchesPassword(password)) {
      return Optional.empty();
    }
    return Optional.of(usuario);
  }

  public Optional<Usuario> login(Context ctx, String email, String password) {
    Optional<Usuario> usuario = authenticate(email, password);
    if (usuario.isEmpty()) {
      return Optional.empty();
    }
    String sessionId = UUID.randomUUID().toString();
    SessionInfo session =
        new SessionInfo(
            sessionId,
            usuario.get().getId(),
            Instant.now().plus(TOKEN_TTL),
            usuario.get().getRoles());
    sesiones.put(sessionId, session);
    String token = crearToken(usuario.get(), sessionId);
    ctx.header("Set-Cookie", cookieHeader(token));
    return usuario;
  }

  public void logout(Context ctx) {
    String token = ctx.cookie(COOKIE_NAME);
    if (token != null && !token.isBlank()) {
      verificar(token).map(jwt -> jwt.getClaim("sid").asString()).ifPresent(sesiones::remove);
    }
    ctx.header("Set-Cookie", clearCookieHeader());
  }

  public Optional<Usuario> currentUser(Context ctx) {
    String token = ctx.cookie(COOKIE_NAME);
    if (token == null || token.isBlank()) {
      return Optional.empty();
    }
    return validarToken(token);
  }

  public boolean hasAccess(Context ctx, Role... required) {
    Optional<Usuario> current = currentUser(ctx);
    if (current.isEmpty()) {
      return false;
    }
    Usuario user = current.get();
    if (user.hasRole(Role.ADMIN)) {
      return true;
    }
    return user.hasAnyRole(required);
  }

  static String hashPassword(String password) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("No se pudo hashear la contraseña.", e);
    }
  }

  private void seedUsers() {
    registrarUsuario(
        new Usuario(
            "u-admin", "admin@donatrack.local", hashPassword("admin123"), Set.of(Role.ADMIN)));
    registrarUsuario(
        new Usuario(
            "u-repartidor",
            "repartidor@donatrack.local",
            hashPassword("repartidor123"),
            Set.of(Role.REPARTIDOR)));
    registrarUsuario(
        new Usuario(
            "u-donante",
            "donante@donatrack.local",
            hashPassword("donante123"),
            Set.of(Role.DONANTE)));
    registrarUsuario(
        new Usuario(
            "u-beneficiario",
            "beneficiario@donatrack.local",
            hashPassword("beneficiario123"),
            Set.of(Role.BENEFICIARIO)));
  }

  private void registrarUsuario(Usuario usuario) {
    usuariosPorEmail.put(normalizarEmail(usuario.getEmail()), usuario);
  }

  private String crearToken(Usuario usuario, String sessionId) {
    return JWT.create()
        .withSubject(usuario.getId())
        .withClaim("email", usuario.getEmail())
        .withClaim("sid", sessionId)
        .withClaim("roles", usuario.getRoles().stream().map(Enum::name).toList())
        .withExpiresAt(Instant.now().plus(TOKEN_TTL))
        .sign(ALGORITHM);
  }

  private Optional<Usuario> validarToken(String token) {
    Optional<DecodedJWT> jwt = verificar(token);
    if (jwt.isEmpty()) {
      return Optional.empty();
    }
    String sessionId = jwt.get().getClaim("sid").asString();
    SessionInfo session = sessionId == null ? null : sesiones.get(sessionId);
    if (session == null || session.expiresAt().isBefore(Instant.now())) {
      if (sessionId != null) {
        sesiones.remove(sessionId);
      }
      return Optional.empty();
    }
    String email = jwt.get().getClaim("email").asString();
    return Optional.ofNullable(email).map(e -> usuariosPorEmail.get(normalizarEmail(e)));
  }

  private static Optional<DecodedJWT> verificar(String token) {
    try {
      return Optional.of(VERIFIER.verify(token));
    } catch (JWTVerificationException e) {
      return Optional.empty();
    }
  }

  private static String normalizarEmail(String email) {
    return email == null ? null : email.trim().toLowerCase();
  }

  private static String cookieHeader(String token) {
    return COOKIE_NAME
        + "="
        + token
        + "; Path=/; Max-Age="
        + TOKEN_TTL.toSeconds()
        + "; HttpOnly; SameSite=Lax";
  }

  private static String clearCookieHeader() {
    return COOKIE_NAME + "=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0";
  }
}
