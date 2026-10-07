package ar.edu.utn.donatrack.logistica;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

/**
 * Parada de una {@link Ruta}: agrupa las donaciones que se entregan en una misma entidad
 * beneficiaria.
 */
@Entity
@Table(schema = "logistica", name = "destinos")
public class Destino {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "value", column = @Column(name = "entidad_id", nullable = false))
  })
  private EntidadBeneficiariaId entidadId;

  @Column(nullable = false)
  private String direccion;

  @ElementCollection
  @CollectionTable(
      schema = "logistica",
      name = "destino_donaciones",
      joinColumns = @JoinColumn(name = "destino_id"))
  @AttributeOverrides({
    @AttributeOverride(name = "value", column = @Column(name = "donacion_id", nullable = false))
  })
  private List<DonacionId> donaciones = new ArrayList<>();

  protected Destino() {}

  public Destino(EntidadBeneficiariaId entidadId, String direccion, List<DonacionId> donaciones) {
    this.entidadId = Objects.requireNonNull(entidadId, "entidadId no debe ser null");
    this.direccion = Objects.requireNonNull(direccion, "direccion no debe ser null");
    Objects.requireNonNull(donaciones, "donaciones no debe ser null");
    for (DonacionId d : donaciones) {
      Objects.requireNonNull(d, "donacion no debe ser null");
    }
    this.donaciones = new ArrayList<>(donaciones);
  }

  public Long getId() {
    return id;
  }

  public EntidadBeneficiariaId entidadId() {
    return entidadId;
  }

  public String direccion() {
    return direccion;
  }

  public List<DonacionId> donaciones() {
    return Collections.unmodifiableList(donaciones);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Destino)) {
      return false;
    }
    Destino destino = (Destino) o;
    return Objects.equals(entidadId, destino.entidadId)
        && Objects.equals(direccion, destino.direccion)
        && Objects.equals(donaciones, destino.donaciones);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entidadId, direccion, donaciones);
  }
}
