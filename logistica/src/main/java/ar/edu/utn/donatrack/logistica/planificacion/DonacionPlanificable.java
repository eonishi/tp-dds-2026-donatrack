package ar.edu.utn.donatrack.logistica.planificacion;

import ar.edu.utn.donatrack.logistica.monitoreo.Coordenada;
import java.util.Objects;
import java.util.UUID;

/**
 * Donación con los datos que necesita el planificador externo.
 *
 * <p>Es la forma en que logística representa la respuesta de {@code GET
 * /api/donaciones?estado=ASIGNACION_REALIZADA} del microservicio de donaciones: alcanza con los
 * identificadores, la dirección de destino, las coordenadas y las medidas de carga, sin conocer el
 * objeto Donacion completo.
 *
 * @param idDonacion identificador de la donación (no nulo)
 * @param idEntidadBeneficiaria entidad beneficiaria de destino (no nula)
 * @param direccionDestino dirección donde se entrega (no nula)
 * @param coordenadas coordenadas del destino (no nulas)
 * @param volumenM3 volumen de la donación en metros cúbicos (no negativo, no NaN)
 * @param pesoKg peso de la donación en kilogramos (no negativo, no NaN)
 */
public record DonacionPlanificable(
    UUID idDonacion,
    Long idEntidadBeneficiaria,
    String direccionDestino,
    Coordenada coordenadas,
    double volumenM3,
    double pesoKg) {

  public DonacionPlanificable {
    Objects.requireNonNull(idDonacion, "idDonacion no debe ser null");
    Objects.requireNonNull(idEntidadBeneficiaria, "idEntidadBeneficiaria no debe ser null");
    Objects.requireNonNull(direccionDestino, "direccionDestino no debe ser null");
    Objects.requireNonNull(coordenadas, "coordenadas no debe ser null");
    if (volumenM3 < 0 || Double.isNaN(volumenM3)) {
      throw new IllegalArgumentException("volumenM3 no debe ser negativo ni NaN: " + volumenM3);
    }
    if (pesoKg < 0 || Double.isNaN(pesoKg)) {
      throw new IllegalArgumentException("pesoKg no debe ser negativo ni NaN: " + pesoKg);
    }
  }
}
