package ar.edu.utn.donatrack.beneficiarios;

import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.bienes.UnidadMedida;
import javax.persistence.Entity;

// El identificador se hereda de Necesidad; volver a declararlo aquí duplicaba el id mapeado.
@Entity
public class NecesidadRecurrente extends Necesidad {

  protected NecesidadRecurrente() {}

  private TipoPeriodo periodo;

  public NecesidadRecurrente(
      SubCategoria subcategoria, double cantidadObjetivo, UnidadMedida unidad, String descripcion) {
    super(subcategoria, cantidadObjetivo, unidad, descripcion);
  }

  public TipoPeriodo getPeriodo() {
    return periodo;
  }

  public void setPeriodo(TipoPeriodo periodo) {
    this.periodo = periodo;
  }

  public void reiniciarPeriodo() {
    this.resetearCantidadRecibida();
  }
}
