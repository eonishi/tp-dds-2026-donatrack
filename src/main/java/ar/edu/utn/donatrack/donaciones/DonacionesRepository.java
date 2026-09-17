package ar.edu.utn.donatrack.donaciones;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.estados.EnDeposito;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DonacionesRepository implements WithSimplePersistenceUnit {

  public long contarDonacionesAsignadas(EntidadBeneficiaria entidad, LocalDate fechaDesde) {
    String jpql =
        "SELECT COUNT(d) FROM Donacion d "
            + "JOIN d.historialEstado h "
            + "WHERE d.entidadBeneficiaria = :entidad "
            + "AND h.estadoAlcanzado IN (:estados) "
            + "AND h.fecha >= :fechaDesde";

    return entityManager()
        .createQuery(jpql, Long.class)
        .setParameter("entidad", entidad)
        .setParameter("estados", List.of(NombreEstado.ASIGNACION_REALIZADA, NombreEstado.ENTREGADA))
        .setParameter("fechaDesde", fechaDesde)
        .getSingleResult();
  }

  private final List<Donacion> donaciones = new ArrayList<>();

  public List<Donacion> getDonaciones() {
    return Collections.unmodifiableList(donaciones);
  }

  public List<Donacion> getDonacionesEnDeposito() {
    return donaciones.stream()
        .filter(donacion -> donacion.getEstado() instanceof EnDeposito)
        .toList();
  }

  public List<Donacion> getDonacionesEnAsignacionRealizada() {
    return donaciones.stream()
        .filter(donacion -> donacion.getNombreEstado() == NombreEstado.ASIGNACION_REALIZADA)
        .toList();
  }

  public void addDonacion(Donacion donacion) {
    this.donaciones.add(donacion);
  }
}
