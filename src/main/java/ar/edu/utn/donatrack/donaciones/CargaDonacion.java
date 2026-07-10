package ar.edu.utn.donatrack.donaciones;

import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.bienes.EstadoUso;
import ar.edu.utn.donatrack.donantes.Donante;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CargaDonacion {
  private final LocalDate fechaRegistro;
  private final DonacionesRepository donacionesRepository;

  public CargaDonacion(LocalDate fechaRegistro, DonacionesRepository donacionesRepository) {
    this.fechaRegistro = fechaRegistro;
    this.donacionesRepository =
        Objects.requireNonNull(donacionesRepository, "donacionesRepository no puede ser null");
  }

  public void cargarDonacion(Donante donante, List<Bien> bienes) {
    bienes.stream()
        .collect(
            Collectors.groupingBy(
                bien ->
                    new SegmentacionKey(
                        bien.getSubcategoria(),
                        bien.getFechaVencimiento(),
                        bien.getEstadoDeUso() != null
                            ? bien.getEstadoDeUso()
                            : EstadoUso.NO_APLICA)))
        .forEach(
            (key, bienesDeClave) -> {
              Donacion donacion = new Donacion(donante, bienesDeClave);
              donacionesRepository.addDonacion(donacion);
            });
  }
}
