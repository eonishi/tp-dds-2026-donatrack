package ar.edu.utn.donatrack.logistica.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;
import java.util.List;

/** Valida las sesiones emitidas por el microservicio de donaciones. */
public final class LogisticaSecurity {
  private static final String COOKIE_NAME = "access_token";
  private static final JWTVerifier VERIFIER =
      JWT.require(
              Algorithm.HMAC256(
                  System.getenv()
                      .getOrDefault("DONATRACK_JWT_SECRET", "donatrack-dev-secret-change-me")))
          .withClaimPresence("exp")
          .build();
  private static final String API_KEY =
      System.getenv().getOrDefault("DONATRACK_API_KEY", "S2S_SECRET_KEY");

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
    DecodedJWT jwt = verifiedToken(ctx);
    if (jwt == null) {
      return false;
    }
    try {
      List<String> roles = jwt.getClaim("roles").asList(String.class);
      return roles != null && roles.contains(required.name());
    } catch (JWTDecodeException e) {
      return false;
    }
  }

  private static boolean hasToken(Context ctx) {
    return verifiedToken(ctx) != null;
  }

  private static DecodedJWT verifiedToken(Context ctx) {
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
      return VERIFIER.verify(token);
    } catch (JWTVerificationException e) {
      return null;
    }
  }
}
