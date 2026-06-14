package ar.edu.utn.donatrack.donaciones;

import ar.edu.utn.donatrack.bienes.EstadoUso;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import java.time.LocalDate;
import java.util.Objects;

public class SegmentacionKey {
  private final SubCategoria subcategoria;
  private final LocalDate fechaVencimiento;
  private final EstadoUso estadoUso;
  private LocalDate fechaMin = null;

  public SegmentacionKey(
      SubCategoria subcategoria, LocalDate fechaVencimiento, EstadoUso estadoUso) {
    this.subcategoria = subcategoria;
    this.fechaVencimiento = fechaVencimiento;
    this.estadoUso = estadoUso;

    if (fechaVencimiento != null) {
      final int rangoDeFecha = 5;
      this.fechaMin = fechaVencimiento.minusDays(rangoDeFecha);
    }
  }

  public SubCategoria getSubcategoria() {
    return subcategoria;
  }

  public LocalDate getFechaVencimiento() {
    return fechaVencimiento;
  }

  public EstadoUso getEstadoUso() {
    return estadoUso;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SegmentacionKey that = (SegmentacionKey) o;
    if (fechaVencimiento == null) {
      return Objects.equals(subcategoria, that.subcategoria) && estadoUso == that.estadoUso;
    } else {
      return Objects.equals(subcategoria, that.subcategoria)
          && fechaMin.isBefore(that.fechaVencimiento)
          && (fechaVencimiento.isAfter(that.fechaVencimiento)
              || fechaVencimiento.isEqual(that.fechaVencimiento));
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(subcategoria, fechaVencimiento, estadoUso);
  }
}
