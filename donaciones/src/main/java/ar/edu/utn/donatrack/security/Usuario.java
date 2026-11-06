package ar.edu.utn.donatrack.security;

import java.util.Objects;
import java.util.Set;

public final class Usuario {
  private final String id;
  private final String email;
  private final String passwordHash;
  private final Set<Role> roles;

  public Usuario(String id, String email, String passwordHash, Set<Role> roles) {
    this.id = Objects.requireNonNull(id);
    this.email = Objects.requireNonNull(email).trim().toLowerCase();
    this.passwordHash = Objects.requireNonNull(passwordHash);
    this.roles = Set.copyOf(Objects.requireNonNull(roles));
  }

  public String getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public Set<Role> getRoles() {
    return roles;
  }

  public String getUsername() {
    return email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
  }

  public boolean isEsAdmin() {
    return hasRole(Role.ADMIN);
  }

  public boolean isRepartidor() {
    return hasRole(Role.REPARTIDOR);
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

  public boolean matchesPassword(String password) {
    return passwordHash.equals(AuthService.hashPassword(password));
  }
}
