package ar.edu.utn.donatrack.logistica;

import java.util.Objects;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Chofer o acompañante de la flota que opera las rutas desde su aplicación.
 *
 * <p>Es un usuario propio de logística: se autentica contra este servicio y donaciones no lo
 * conoce. Una ruta puede tener varios repartidores asignados, y cualquiera de ellos puede iniciarla
 * o finalizarla.
 */
@Entity
@Table(name = "repartidores")
public class Repartidor {

  // Lo asigna el proveedor al persistir, igual que el resto de las entidades.
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(nullable = false)
  private String nombre;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String hashContrasenia;

  protected Repartidor() {}

  public Repartidor(String nombre, String email, String contrasenia) {
    this.nombre = Objects.requireNonNull(nombre, "nombre no debe ser nulo");
    this.email = normalizarEmail(Objects.requireNonNull(email, "email no debe ser nulo"));
    this.hashContrasenia =
        hashear(Objects.requireNonNull(contrasenia, "contraseña no debe ser nula"));
  }

  /**
   * @return el id asignado por la base, o {@code null} si todavía no se persistió
   */
  public UUID getId() {
    return id;
  }

  public String getNombre() {
    return nombre;
  }

  public String getEmail() {
    return email;
  }

  public boolean tieneContrasenia(String contrasenia) {
    if (contrasenia == null) {
      return false;
    }
    try {
      return BCrypt.checkpw(contrasenia, hashContrasenia);
    } catch (IllegalArgumentException e) {
      // El hash guardado no es BCrypt: no coincide.
      return false;
    }
  }

  static String normalizarEmail(String email) {
    return email.trim().toLowerCase();
  }

  // BCrypt genera una sal aleatoria por contraseña y la guarda dentro del hash.
  private static String hashear(String contrasenia) {
    return BCrypt.hashpw(contrasenia, BCrypt.gensalt(10));
  }

  // Identidad por email: es único y, a diferencia del id, existe antes de persistir.
  @Override
  public boolean equals(Object o) {
    return o instanceof Repartidor otro && email.equals(otro.email);
  }

  @Override
  public int hashCode() {
    return email.hashCode();
  }
}
