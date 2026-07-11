package ar.edu.utn.donatrack.logistica;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donaciones.Donacion;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Agrupa Donaciones asignadas a una misma EntidadBeneficiaria dentro de una Ruta logística.
 *
 * <p>Value Object mutable sin identidad global — su identidad es contextual dentro del agregado
 * Ruta. Cada Destino pertenece a una única EntidadBeneficiaria y contiene la lista de Donaciones
 * asignadas a ese destino.
 *
 * <p>Las Donaciones se agregan mediante {@code agregarDonacion}, que valida que la entidad
 * beneficiaria de la donación coincida con la de este Destino. La lista de donaciones se expone
 * como copia inmodificable para proteger la integridad interna.
 */
public class Destino {

  private final EntidadBeneficiaria entidadBeneficiaria;
  private final List<Donacion> donaciones;

  /**
   * Crea un nuevo Destino para la entidad beneficiaria especificada.
   *
   * @param entidadBeneficiaria entidad beneficiaria asociada a este destino (no nula)
   * @throws NullPointerException si {@code entidadBeneficiaria} es {@code null}
   */
  public Destino(EntidadBeneficiaria entidadBeneficiaria) {
    this.entidadBeneficiaria =
        Objects.requireNonNull(entidadBeneficiaria, "entidadBeneficiaria no debe ser nula");
    this.donaciones = new ArrayList<>();
  }

  /**
   * Agrega una donación a este destino.
   *
   * <p>La donación debe pertenecer a la misma entidad beneficiaria que este destino (comparación
   * mediante {@link Objects#equals}). Si la entidad no coincide, se rechaza la operación.
   *
   * <p>No se realiza ninguna guarda contra donaciones duplicadas — la consistencia contra
   * duplicados se maneja a nivel del agregado Ruta.
   *
   * @param donacion la donación a agregar (no nula)
   * @throws NullPointerException si {@code donacion} es {@code null}
   * @throws IllegalArgumentException si {@code donacion.getEntidadBeneficiaria()} no es {@code
   *     equals} a la entidad de este destino
   */
  public void agregarDonacion(Donacion donacion) {
    Objects.requireNonNull(donacion, "donacion no debe ser nula");
    if (!Objects.equals(donacion.getEntidadBeneficiaria(), this.entidadBeneficiaria)) {
      throw new IllegalArgumentException(
          "la donación no pertenece a la entidad beneficiaria: esperada="
              + entidadBeneficiaria.getRazonSocial()
              + ", recibida="
              + (donacion.getEntidadBeneficiaria() != null
                  ? donacion.getEntidadBeneficiaria().getRazonSocial()
                  : "null"));
    }
    this.donaciones.add(donacion);
  }

  /**
   * @return la entidad beneficiaria asociada a este destino
   */
  public EntidadBeneficiaria getEntidadBeneficiaria() {
    return entidadBeneficiaria;
  }

  /**
   * @return copia inmodificable de la lista de donaciones asignadas a este destino
   */
  public List<Donacion> getDonaciones() {
    return Collections.unmodifiableList(new ArrayList<>(donaciones));
  }

  // ── Identity based on EntidadBeneficiaria ──────────────

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Destino that = (Destino) o;
    return Objects.equals(entidadBeneficiaria, that.entidadBeneficiaria);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entidadBeneficiaria);
  }
}
