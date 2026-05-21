package ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones;

import ar.edu.utn.donatrack.BienesYCategorias.SubCategoria;
import ar.edu.utn.donatrack.BienesYCategorias.EstadoUso;
import java.time.LocalDate;
import java.util.Objects;

public class SegmentacionKey {
  private final SubCategoria subcategoria;
  private final LocalDate fechaVencimiento;
  private final EstadoUso estadoUso;

  public SegmentacionKey(SubCategoria subcategoria, LocalDate fechaVencimiento, EstadoUso estadoUso) {
    this.subcategoria = subcategoria;
    this.fechaVencimiento = fechaVencimiento;
    this.estadoUso = estadoUso;
  }

  public SubCategoria getSubcategoria() { return subcategoria; }
  public LocalDate getFechaVencimiento() { return fechaVencimiento; }
  public EstadoUso getEstadoUso() { return estadoUso; }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SegmentacionKey that = (SegmentacionKey) o;
    return Objects.equals(subcategoria, that.subcategoria) &&
        Objects.equals(fechaVencimiento, that.fechaVencimiento) &&
        estadoUso == that.estadoUso;
  }

  @Override
  public int hashCode() {
    return Objects.hash(subcategoria, fechaVencimiento, estadoUso);
  }
}