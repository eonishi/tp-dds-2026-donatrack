package ar.edu.utn.donatrack.security;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

/**
 * Cuenta de acceso a la web de donaciones.
 *
 * <p>La persona donante y la entidad beneficiaria quedan vinculadas a su registro del dominio por
 * id, para que cada una vea solo sus donaciones. La persona administradora no tiene vínculo.
 */
@Entity
@Table(name = "usuarios")
public class Usuario {
  // Lo asigna el proveedor al persistir, igual que el resto de las entidades.
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String passwordHash;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "usuario_roles", joinColumns = @JoinColumn(name = "usuario_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "rol")
  private Set<Role> roles = new HashSet<>();

  private UUID donanteId;

  private Long entidadId;

  protected Usuario() {}

  public Usuario(String email, String passwordHash, Set<Role> roles) {
    this(email, passwordHash, roles, null, null);
  }

  public Usuario(
      String email, String passwordHash, Set<Role> roles, UUID donanteId, Long entidadId) {
    this.email = Objects.requireNonNull(email).trim().toLowerCase();
    this.passwordHash = Objects.requireNonNull(passwordHash);
    this.roles = new HashSet<>(Objects.requireNonNull(roles));
    this.donanteId = donanteId;
    this.entidadId = entidadId;
  }

  /**
   * @return el id asignado por la base, o {@code null} si todavía no se persistió
   */
  public UUID getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public Set<Role> getRoles() {
    return Set.copyOf(roles);
  }

  public Optional<UUID> getDonanteId() {
    return Optional.ofNullable(donanteId);
  }

  public Optional<Long> getEntidadId() {
    return Optional.ofNullable(entidadId);
  }

  void vincularEntidad(Long entidadId) {
    this.entidadId = Objects.requireNonNull(entidadId);
  }

  public String getUsername() {
    return email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
  }

  public boolean isEsAdmin() {
    return hasRole(Role.ADMIN);
  }

  public boolean isEsDonante() {
    return hasRole(Role.DONANTE);
  }

  public boolean isEsEntidad() {
    return hasRole(Role.BENEFICIARIO);
  }

  public boolean hasRole(Role role) {
    return roles.contains(role);
  }

  public boolean hasAnyRole(Role... expected) {
    for (Role role : expected) {
      if (roles.contains(role)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Cuenta creada por la importación de donantes: todavía no tiene contraseña y la persona la elige
   * al activarla. Ninguna contraseña coincide con el marcador.
   */
  public boolean isPendienteDeActivacion() {
    return passwordHash.startsWith(AuthService.MARCADOR_PENDIENTE);
  }

  void definirContrasena(String nuevoHash) {
    this.passwordHash = Objects.requireNonNull(nuevoHash);
  }

  public boolean matchesPassword(String password) {
    return AuthService.coincide(password, passwordHash);
  }
}
