package ar.edu.utn.donatrack.security;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.http.Context;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Cuentas de la web de donaciones: alta, activación e inicio de sesión. Los usuarios se guardan en
 * la base; la sesión abierta (cookie, token y vencimiento) la maneja {@link Sesiones}.
 */
public class AuthService implements WithSimplePersistenceUnit {
  // BCrypt genera una sal aleatoria por contraseña y la guarda dentro del hash, así dos usuarios
  // con la misma contraseña tienen hashes distintos. El costo 10 es el valor por defecto.
  private static final int COSTO_BCRYPT = 10;

  private final UsuariosRepository usuarios;
  private final Sesiones sesiones = new Sesiones();

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

  /**
   * Dentro de la transacción del llamador, pasa la cuenta de la persona donante al correo nuevo:
   * desde ese momento entra con él. Si no tiene cuenta, no hace nada.
   *
   * @throws IllegalArgumentException si otra cuenta ya usa ese correo
   */
  public void cambiarCorreoDeDonante(UUID donanteId, String correo) {
    usuarios
        .buscarPorDonantes(List.of(donanteId))
        .forEach(
            cuenta -> {
              if (!cuenta.getEmail().equals(correo) && usuarios.existeEmail(correo)) {
                throw new IllegalArgumentException("Ya existe un usuario con el email " + correo);
              }
              cuenta.cambiarEmail(correo);
            });
  }

  /** Dentro de la transacción del llamador, borra la cuenta de la persona donante, si tiene. */
  public void eliminarCuentaDeDonante(UUID donanteId) {
    usuarios.buscarPorDonantes(List.of(donanteId)).forEach(this::remove);
  }

  static final String MARCADOR_PENDIENTE = "!pendiente-de-activacion";

  /** La cuenta de cada donante que tiene una, para mostrar su estado en los listados. */
  public Map<UUID, Usuario> cuentasDeDonantes(Collection<UUID> donantes) {
    Map<UUID, Usuario> cuentas = new HashMap<>();
    usuarios
        .buscarPorDonantes(donantes)
        .forEach(u -> u.getDonanteId().ifPresent(id -> cuentas.putIfAbsent(id, u)));
    return cuentas;
  }

  /**
   * Crea, dentro de la transacción del llamador, la cuenta de una persona donante importada o dada
   * de alta por la administración. Queda pendiente de activación: sin contraseña, hasta que la
   * persona la elija en {@code /activar}.
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

  /**
   * Abre la sesión del usuario y le deja la cookie. La usan también el registro y la activación.
   */
  public void abrirSesion(Context ctx, Usuario usuario) {
    sesiones.abrir(ctx, usuario);
  }

  public void logout(Context ctx) {
    sesiones.cerrar(ctx);
  }

  /**
   * Quién tiene la sesión abierta en este request. En las rutas protegidas ya lo resolvió {@link
   * ControlDeAcceso}; si no, se busca por la cookie (las páginas públicas lo usan para el menú).
   */
  public Optional<Usuario> currentUser(Context ctx) {
    return ControlDeAcceso.usuario(ctx)
        .or(() -> sesiones.usuarioId(ctx).flatMap(usuarios::buscarPorId));
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
    vincularEntidadDePrueba();
  }

  private void vincularEntidadDePrueba() {
    withTransaction(
        () -> {
          Usuario usuario = usuarios.buscarPorEmail("beneficiario@donatrack.local").orElseThrow();
          if (!usuario.hasRole(Role.BENEFICIARIO)
              || usuario
                  .getEntidadId()
                  .filter(id -> find(EntidadBeneficiaria.class, id) != null)
                  .isPresent()) {
            return;
          }
          EntidadBeneficiaria entidad =
              new EntidadBeneficiaria("Entidad beneficiaria de ejemplo", "", "");
          entidad.agregarCorreo(new CorreoElectronico(usuario.getEmail()));
          persist(entidad);
          usuario.vincularEntidad(entidad.getId());
        });
  }

  // Los usuarios de prueba se crean una sola vez: se conservan sus credenciales y roles.
  private void registrarUsuario(String email, String password, Role rol) {
    if (!usuarios.existeEmail(email)) {
      Usuario usuario = new Usuario(email, hashPassword(password), Set.of(rol));
      withTransaction(() -> usuarios.agregar(usuario));
    }
  }
}
