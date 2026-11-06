package ar.edu.utn.donatrack.logistica.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Valida las sesiones emitidas por el microservicio de donaciones. */
public final class LogisticaSecurity {
  private static final String COOKIE_NAME = "access_token";
  private static final String JWT_SECRET =
      System.getenv().getOrDefault("DONATRACK_JWT_SECRET", "donatrack-dev-secret-change-me");
  private static final String API_KEY =
      System.getenv().getOrDefault("DONATRACK_API_KEY", "S2S_SECRET_KEY");
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private LogisticaSecurity() {}

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
    return API_KEY.equals(ctx.header("Authorization"));
  }

  private static boolean hasRole(Context ctx, Role required) {
    Map<String, Object> claims = claims(ctx);
    if (claims == null) {
      return false;
    }
    Object roles = claims.get("roles");
    if (!(roles instanceof Collection<?> values)) {
      return false;
    }
    return values.stream().anyMatch(value -> required.name().equals(String.valueOf(value)));
  }

  private static boolean hasToken(Context ctx) {
    return claims(ctx) != null;
  }

  private static Map<String, Object> claims(Context ctx) {
    String token = ctx.cookie(COOKIE_NAME);
    if (token == null || token.isBlank()) {
      String authorization = ctx.header("Authorization");
      if (authorization != null && authorization.startsWith("Bearer ")) {
        token = authorization.substring("Bearer ".length());
      }
    }
    if (token == null || token.isBlank()) {
      return null;
    }

    try {
      String[] parts = token.split("\\.");
      if (parts.length != 3) {
        return null;
      }
      byte[] expected = hmacSha256(parts[0] + "." + parts[1]);
      byte[] actual = Base64.getUrlDecoder().decode(parts[2]);
      if (!MessageDigest.isEqual(expected, actual)) {
        return null;
      }
      Map<String, Object> claims =
          MAPPER.readValue(
              new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8),
              Map.class);
      Object expiration = claims.get("exp");
      if (!(expiration instanceof Number)
          || Instant.now().getEpochSecond() > ((Number) expiration).longValue()) {
        return null;
      }
      return claims;
    } catch (Exception e) {
      return null;
    }
  }

  private static byte[] hmacSha256(String value) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(JWT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
  }
}
