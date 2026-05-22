package ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones;

import ar.edu.utn.donatrack.BienesYCategorias.Bien;
import ar.edu.utn.donatrack.BienesYCategorias.EstadoUso;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CargaDonacion {
  private final LocalDate fechaRegistro;
  private final List<Donacion> donaciones = new ArrayList<>();

  public CargaDonacion(LocalDate fechaRegistro) {
    this.fechaRegistro = fechaRegistro;
  }

  public void cargarDonacion(List<Bien> bienes) {
    bienes.stream()
        .collect(Collectors.groupingBy(bien -> new SegmentacionKey(
            bien.getSubcategoria(),
            bien.esPerecedero() ? bien.getFechaVencimiento() : null,
            bien.getEstadoDeUso() != null ? bien.getEstadoDeUso() : EstadoUso.NO_APLICA
        )))
        .forEach((key, bienesDeClave) -> {
          Donacion donacion = new Donacion(bienesDeClave);
          donaciones.add(donacion);
        });
  }

  public List<Donacion> getDonaciones() {
    return Collections.unmodifiableList(donaciones);
  }
}