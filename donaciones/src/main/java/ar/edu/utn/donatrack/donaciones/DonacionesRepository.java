package ar.edu.utn.donatrack.donaciones;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.estados.EnDeposito;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DonacionesRepository {
  private final Map<DonacionId, Donacion> donaciones = new LinkedHashMap<>();

  public synchronized List<Donacion> getDonaciones() {
    return Collections.unmodifiableList(new ArrayList<>(donaciones.values()));
  }

  public List<Donacion> getDonacionesEnDeposito() {
    return donaciones.values().stream()
        .filter(donacion -> donacion.getEstado() instanceof EnDeposito)
        .toList();
  }

  public List<Donacion> getDonacionesEnAsignacionRealizada() {
    return donaciones.values().stream()
        .filter(donacion -> donacion.getNombreEstado() == NombreEstado.ASIGNACION_REALIZADA)
        .toList();
  }

  public synchronized void addDonacion(Donacion donacion) {
    save(donacion);
  }

  public synchronized Donacion save(Donacion donacion) {
    donaciones.put(donacion.getId(), donacion);
    return donacion;
  }

  public synchronized Optional<Donacion> findById(DonacionId id) {
    return Optional.ofNullable(donaciones.get(id));
  }

  public synchronized void deleteById(DonacionId id) {
    donaciones.remove(id);
  }

  /**
   * Cuenta las donaciones entregadas a la entidad desde la fecha indicada (inclusive).
   *
   * @param entidad entidad beneficiaria receptora
   * @param fechaDesde fecha mínima de entrega
   * @return cantidad de donaciones con un evento {@link NombreEstado#ENTREGADA} en el período
   */
  public long cantidadEntregadasDesde(EntidadBeneficiaria entidad, LocalDate fechaDesde) {
    return donaciones.values().stream()
        .filter(donacion -> entidad.equals(donacion.getEntidadBeneficiaria()))
        .filter(
            donacion ->
                donacion.getHistorialEstado().stream()
                    .anyMatch(
                        historial ->
                            historial.getEstadoAlcanzado() == NombreEstado.ENTREGADA
                                && !historial.getFecha().toLocalDate().isBefore(fechaDesde)))
        .count();
  }

  /**
   * Cuenta todas las donaciones entregadas a la entidad, sin recorte de fecha.
   *
   * <p>{@link #cantidadEntregadasDesde(EntidadBeneficiaria, LocalDate)} exige una cota inferior;
   * usarla con {@link LocalDate#MIN} para expresar un total escondería la intención. Este método
   * conserva la misma regla de entrega ({@link NombreEstado#ENTREGADA}) para el conteo total.
   *
   * @param entidad entidad beneficiaria receptora
   * @return cantidad de donaciones entregadas a la entidad
   */
  public long cantidadEntregadas(EntidadBeneficiaria entidad) {
    return donaciones.values().stream()
        .filter(donacion -> entidad.equals(donacion.getEntidadBeneficiaria()))
        .filter(
            donacion ->
                donacion.getHistorialEstado().stream()
                    .anyMatch(
                        historial -> historial.getEstadoAlcanzado() == NombreEstado.ENTREGADA))
        .count();
  }
}
