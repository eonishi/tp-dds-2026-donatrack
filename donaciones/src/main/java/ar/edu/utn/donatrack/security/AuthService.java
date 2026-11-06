package ar.edu.utn.donatrack.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class AuthService {
  private static final String COOKIE_NAME = "access_token";
  private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String JWT_SECRET =
      System.getenv().getOrDefault("DONATRACK_JWT_SECRET", "donatrack-dev-secret-change-me");

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
      Optional<String> sessionId = sessionId(token);
      sessionId.ifPresent(sesiones::remove);
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
    try {
      String header = base64UrlEncode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
      Map<String, Object> claims = new LinkedHashMap<>();
      claims.put("sub", usuario.getId());
      claims.put("email", usuario.getEmail());
      claims.put("sid", sessionId);
      claims.put("roles", usuario.getRoles().stream().map(Enum::name).toList());
      claims.put("exp", Instant.now().plus(TOKEN_TTL).getEpochSecond());
      String payload = base64UrlEncode(MAPPER.writeValueAsString(claims));
      String signingInput = header + "." + payload;
      String signature = base64UrlEncode(hmacSha256(signingInput));
      return signingInput + "." + signature;
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("No se pudo serializar el token JWT.", e);
    }
  }

  private Optional<Usuario> validarToken(String token) {
    String[] partes = token.split("\\.");
    if (partes.length != 3) {
      return Optional.empty();
    }
    String signingInput = partes[0] + "." + partes[1];
    byte[] expected = hmacSha256(signingInput);
    byte[] actual = Base64.getUrlDecoder().decode(partes[2]);
    if (!MessageDigest.isEqual(expected, actual)) {
      return Optional.empty();
    }
    try {
      String payloadJson =
          new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
      Map<String, Object> claims = MAPPER.readValue(payloadJson, Map.class);
      long exp = ((Number) claims.get("exp")).longValue();
      if (Instant.now().getEpochSecond() > exp) {
        return Optional.empty();
      }
      String sessionId = (String) claims.get("sid");
      SessionInfo session = sesiones.get(sessionId);
      if (session == null || session.expiresAt().isBefore(Instant.now())) {
        if (sessionId != null) {
          sesiones.remove(sessionId);
        }
        return Optional.empty();
      }
      String email = (String) claims.get("email");
      Usuario usuario = usuariosPorEmail.get(normalizarEmail(email));
      if (usuario == null) {
        return Optional.empty();
      }
      return Optional.of(usuario);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  private Optional<String> sessionId(String token) {
    String[] partes = token.split("\\.");
    if (partes.length != 3) {
      return Optional.empty();
    }
    try {
      String payloadJson =
          new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
      Map<String, Object> claims = MAPPER.readValue(payloadJson, Map.class);
      return Optional.ofNullable((String) claims.get("sid"));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  private static String normalizarEmail(String email) {
    return email == null ? null : email.trim().toLowerCase();
  }

  private static String base64UrlEncode(String raw) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
  }

  private static String base64UrlEncode(byte[] raw) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
  }

  private static byte[] hmacSha256(String raw) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(JWT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return mac.doFinal(raw.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new IllegalStateException("No se pudo firmar el JWT.", e);
    }
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
