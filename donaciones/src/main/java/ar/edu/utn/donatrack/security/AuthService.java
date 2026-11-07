package ar.edu.utn.donatrack.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.http.Context;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Login y sesiones de la web de donaciones.
 *
 * <p>Los usuarios se persisten en la base. Las sesiones viven en memoria a propósito: duran 30
 * minutos y reiniciar el servicio solo obliga a volver a iniciar sesión.
 */
public class AuthService implements WithSimplePersistenceUnit {
  private static final String COOKIE_NAME = "access_token";
  private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
  private static final Algorithm ALGORITHM =
      Algorithm.HMAC256(
          System.getenv().getOrDefault("DONATRACK_JWT_SECRET", "donatrack-dev-secret-change-me"));
  private static final JWTVerifier VERIFIER =
      JWT.require(ALGORITHM).withClaimPresence("exp").build();
  // En el despliegue con HTTPS la cookie se marca Secure para que nunca viaje en texto plano.
  private static final boolean COOKIE_SECURE =
      "true".equalsIgnoreCase(System.getenv("DONATRACK_COOKIE_SECURE"));
  // BCrypt genera una sal aleatoria por contraseña y la guarda dentro del hash, así dos usuarios
  // con la misma contraseña tienen hashes distintos. El costo 10 es el valor por defecto.
  private static final int COSTO_BCRYPT = 10;

  private final UsuariosRepository usuarios;
  private final Map<String, SessionInfo> sesiones = new ConcurrentHashMap<>();

  public AuthService() {
    this(new UsuariosRepository());
  }

  public AuthService(UsuariosRepository usuarios) {
    this.usuarios = usuarios;
    seedUsers();
  }

  /**
   * Da de alta un usuario vinculado a su persona donante o entidad beneficiaria.
   *
   * @throws IllegalArgumentException si el email ya tiene un usuario
   */
  public Usuario registrar(
      String email, String password, Set<Role> roles, UUID donanteId, Long entidadId) {
    return withTransaction(() -> agregarUsuario(email, password, roles, donanteId, entidadId));
  }

  /**
   * Igual que {@link #registrar}, pero dentro de la transacción del llamador: lo usa el registro
   * para crear la persona donante o la entidad y su usuario juntos, o ninguno de los dos.
   */
  public Usuario agregarUsuario(
      String email, String password, Set<Role> roles, UUID donanteId, Long entidadId) {
    if (usuarios.existeEmail(email)) {
      throw new IllegalArgumentException("Ya existe un usuario con el email " + email);
    }
    Usuario usuario = new Usuario(email, hashPassword(password), roles, donanteId, entidadId);
    usuarios.agregar(usuario);
    return usuario;
  }

  static final String MARCADOR_PENDIENTE = "!pendiente-de-activacion";

  /**
   * Crea, dentro de la transacción del llamador, la cuenta de una persona donante importada. Queda
   * pendiente de activación: sin contraseña, hasta que la persona la elija en {@code /activar}.
   *
   * @return {@code false} si el correo ya tenía usuario (no se toca)
   */
  public boolean agregarPendiente(String email, UUID donanteId) {
    if (usuarios.existeEmail(email)) {
      return false;
    }
    usuarios.agregar(new Usuario(email, MARCADOR_PENDIENTE, Set.of(Role.DONANTE), donanteId, null));
    return true;
  }

  /**
   * Activa una cuenta importada: la persona prueba quién es con el número de documento que figura
   * en su registro de donante y elige su contraseña.
   *
   * @param documentoDe devuelve el número de documento del donante vinculado a la cuenta
   * @return el usuario activado, o vacío si el correo no tiene una cuenta pendiente o el documento
   *     no coincide (no se distingue cuál de los dos, para no revelar qué correos existen)
   */
  public Optional<Usuario> activar(
      String email,
      String documento,
      String password,
      Function<UUID, Optional<String>> documentoDe) {
    if (email == null || documento == null || password == null) {
      return Optional.empty();
    }
    return withTransaction(
        () ->
            usuarios
                .buscarPorEmail(email)
                .filter(Usuario::isPendienteDeActivacion)
                .filter(
                    u ->
                        u.getDonanteId()
                            .flatMap(documentoDe)
                            .map(doc -> doc.trim().equalsIgnoreCase(documento.trim()))
                            .orElse(false))
                .map(
                    u -> {
                      u.definirContrasena(hashPassword(password));
                      return u;
                    }));
  }

  public boolean existeUsuario(String email) {
    return usuarios.existeEmail(email);
  }

  public Optional<Usuario> authenticate(String email, String password) {
    if (email == null || password == null) {
      return Optional.empty();
    }
    return usuarios.buscarPorEmail(email).filter(u -> u.matchesPassword(password));
  }

  public Optional<Usuario> login(Context ctx, String email, String password) {
    Optional<Usuario> usuario = authenticate(email, password);
    usuario.ifPresent(u -> abrirSesion(ctx, u));
    return usuario;
  }

  /** Abre la sesión del usuario y le deja la cookie. La usa también el registro. */
  public void abrirSesion(Context ctx, Usuario usuario) {
    String sessionId = UUID.randomUUID().toString();
    sesiones.put(
        sessionId,
        new SessionInfo(
            sessionId, usuario.getId(), Instant.now().plus(TOKEN_TTL), usuario.getRoles()));
    ctx.header("Set-Cookie", cookieHeader(crearToken(usuario, sessionId)));
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
    return BCrypt.hashpw(password, BCrypt.gensalt(COSTO_BCRYPT));
  }

  static boolean coincide(String password, String hash) {
    if (password == null || hash == null) {
      return false;
    }
    try {
      return BCrypt.checkpw(password, hash);
    } catch (IllegalArgumentException e) {
      // El hash guardado no es BCrypt (por ejemplo, uno viejo en SHA-256): no coincide.
      return false;
    }
  }

  private void seedUsers() {
    registrarUsuario("admin@donatrack.local", "admin123", Role.ADMIN);
    registrarUsuario("donante@donatrack.local", "donante123", Role.DONANTE);
    registrarUsuario("beneficiario@donatrack.local", "beneficiario123", Role.BENEFICIARIO);
  }

  // Los usuarios de prueba se crean una sola vez: si ya están en la base no se tocan.
  private void registrarUsuario(String email, String password, Role rol) {
    if (!usuarios.existeEmail(email)) {
      Usuario usuario = new Usuario(email, hashPassword(password), Set.of(rol));
      withTransaction(() -> usuarios.agregar(usuario));
    }
  }

  private String crearToken(Usuario usuario, String sessionId) {
    return JWT.create()
        .withSubject(usuario.getId().toString())
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
    // La sesión es la fuente de verdad: el token tiene que ser de la misma persona a la que se le
    // abrió la sesión, y el usuario se carga por el id guardado en ella.
    if (!session.userId().toString().equals(jwt.get().getSubject())) {
      return Optional.empty();
    }
    return usuarios.buscarPorId(session.userId());
  }

  private static Optional<DecodedJWT> verificar(String token) {
    try {
      return Optional.of(VERIFIER.verify(token));
    } catch (JWTVerificationException e) {
      return Optional.empty();
    }
  }

  private static String cookieHeader(String token) {
    return COOKIE_NAME
        + "="
        + token
        + "; Path=/; Max-Age="
        + TOKEN_TTL.toSeconds()
        + "; HttpOnly; SameSite=Lax"
        + (COOKIE_SECURE ? "; Secure" : "");
  }

  private static String clearCookieHeader() {
    return COOKIE_NAME + "=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0";
  }
}
