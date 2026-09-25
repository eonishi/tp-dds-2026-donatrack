package ar.edu.utn.donatrack.logistica;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

/**
 * Agregado raíz que representa una ruta de entrega.
 *
 * <p>Agrupa un {@link Camion} y una lista de {@link Destino} con un ciclo de vida de {@code
 * enCurso}. Su identidad es sintética mediante {@link UUID}.
 *
 * <p>La lista de destinos no admite entidades beneficiarias repetidas: la validación se apoya en la
 * identidad declarada por {@link Destino#entidadId()}.
 */
@Entity
@Table(schema = "logistica", name = "rutas")
public class Ruta {

  @Id private UUID id;

  @ManyToOne
  @JoinColumn(name = "camion_id")
  private Camion camion;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "ruta_id")
  @OrderColumn(name = "orden")
  private List<Destino> destinos;

  private boolean enCurso;

  protected Ruta() {}

  /**
   * Crea una nueva ruta con el camión y los destinos especificados.
   *
   * <p>Genera un {@link UUID} aleatorio como identidad. La ruta se crea con {@code enCurso =
   * false}. La lista de destinos se copia defensivamente.
   *
   * @param camion el camión asignado a la ruta (no nulo)
   * @param destinos la lista de destinos a visitar (no nula)
   * @throws NullPointerException si {@code camion} o {@code destinos} es {@code null}
   * @throws IllegalArgumentException si la lista de destinos contiene entidades beneficiarias
   *     duplicadas
   */
  public Ruta(Camion camion, List<Destino> destinos) {
    this.id = UUID.randomUUID();
    this.camion = Objects.requireNonNull(camion, "camion no debe ser nulo");
    Objects.requireNonNull(destinos, "destinos no debe ser nula");

    long entidadesDistintas = destinos.stream().map(Destino::entidadId).distinct().count();
    if (entidadesDistintas != destinos.size()) {
      throw new IllegalArgumentException(
          "la lista de destinos contiene entidades beneficiarias duplicadas");
    }

    this.destinos = new ArrayList<>(destinos);
    this.enCurso = false;
  }

  /**
   * @return el identificador único de la ruta
   */
  public UUID getId() {
    return id;
  }

  /**
   * @return el camión asignado a esta ruta
   */
  public Camion getCamion() {
    return camion;
  }

  /**
   * @return copia inmodificable de la lista de destinos, preservando el orden original
   */
  public List<Destino> getDestinos() {
    return Collections.unmodifiableList(new ArrayList<>(destinos));
  }

  /**
   * @return {@code true} si la ruta está actualmente en curso
   */
  public boolean isEnCurso() {
    return enCurso;
  }

  // ── Lifecycle ───────────────────────────────────────

  /**
   * Inicia la ruta, cambiando el estado a {@code enCurso = true}.
   *
   * @throws IllegalStateException si la ruta ya está en curso
   */
  public void iniciar() {
    if (enCurso) {
      throw new IllegalStateException("la ruta ya está en curso");
    }
    this.enCurso = true;
    // Aca faltaria que se comunique con la parte de las donaciones para cambiar el estado a en
    // curso
  }

  /**
   * Finaliza la ruta, cambiando el estado a {@code enCurso = false}.
   *
   * @throws IllegalStateException si la ruta no está en curso
   */
  public void finalizar() {
    if (!enCurso) {
      throw new IllegalStateException("la ruta no está en curso");
    }
    this.enCurso = false;
  }

  // ── Identity based on UUID  ─────────────────────────────────

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Ruta that = (Ruta) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
