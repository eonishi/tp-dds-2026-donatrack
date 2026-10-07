package ar.edu.utn.donatrack.logistica.planificacion;

import ar.edu.utn.donatrack.logistica.Camion;
import ar.edu.utn.donatrack.logistica.CamionRepository;
import ar.edu.utn.donatrack.logistica.Destino;
import ar.edu.utn.donatrack.logistica.DonacionId;
import ar.edu.utn.donatrack.logistica.Ruta;
import ar.edu.utn.donatrack.logistica.RutaRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Orquesta la planificación de rutas.
 *
 * <p>Toma las donaciones en estado {@code ASIGNACION_REALIZADA} a través de {@link
 * DonacionesRemotas}, las envía al planificador externo en lotes de hasta {@value
 * LoteDonaciones#MAXIMO_DONACIONES} donaciones junto con los camiones disponibles y, cuando llega
 * el resultado, materializa las {@link Ruta} y avisa al microservicio de donaciones qué donaciones
 * pasaron a {@code LISTA_PARA_ENTREGAR}.
 *
 * <p>Cada lote enviado queda pendiente hasta que llega su resultado: el resultado se valida contra
 * las donaciones de ese lote antes de tocar el repositorio de rutas.
 */
public class ServicioPlanificacionRutas {

  private final DonacionesRemotas donacionesRemotas;
  private final CamionRepository camionRepository;
  private final PlanificadorRutas planificadorRutas;
  private final RutaRepository rutaRepository;
  private final Map<LoteId, List<DonacionPlanificable>> lotesPendientes = new HashMap<>();

  /**
   * Crea el servicio de planificación.
   *
   * @param donacionesRemotas puerto hacia el microservicio de donaciones (no nulo)
   * @param camionRepository repositorio de camiones disponibles (no nulo)
   * @param planificadorRutas planificador externo de rutas (no nulo)
   * @param rutaRepository repositorio donde se guardan las rutas armadas (no nulo)
   */
  public ServicioPlanificacionRutas(
      DonacionesRemotas donacionesRemotas,
      CamionRepository camionRepository,
      PlanificadorRutas planificadorRutas,
      RutaRepository rutaRepository) {
    this.donacionesRemotas =
        Objects.requireNonNull(donacionesRemotas, "donacionesRemotas no debe ser null");
    this.camionRepository =
        Objects.requireNonNull(camionRepository, "camionRepository no debe ser null");
    this.planificadorRutas =
        Objects.requireNonNull(planificadorRutas, "planificadorRutas no debe ser null");
    this.rutaRepository = Objects.requireNonNull(rutaRepository, "rutaRepository no debe ser null");
  }

  /**
   * Envía al planificador externo las donaciones pendientes de asignación, en lotes.
   *
   * @return los identificadores de los lotes enviados, o lista vacía si no hay donaciones o
   *     camiones
   */
  public List<LoteId> planificar() {
    List<DonacionPlanificable> disponibles = donacionesRemotas.listarEnAsignacionRealizada();
    List<Camion> camiones = camionRepository.listarTodos();

    if (disponibles.isEmpty() || camiones.isEmpty()) {
      return List.of();
    }

    List<LoteId> loteIds = new ArrayList<>();
    List<List<DonacionPlanificable>> batches =
        partirEnLotes(disponibles, LoteDonaciones.MAXIMO_DONACIONES);
    List<List<Camion>> camionesPorLote = partirCamionesEnLotes(camiones, batches.size());

    for (int i = 0; i < batches.size(); i++) {
      List<DonacionPlanificable> batch = batches.get(i);
      List<Camion> camionesAsignados = camionesPorLote.get(i);

      if (camionesAsignados.isEmpty()) {
        continue;
      }

      LoteDonaciones lote = new LoteDonaciones(batch, camionesAsignados);
      LoteId loteId = planificadorRutas.enviarLote(lote);
      lotesPendientes.put(loteId, List.copyOf(batch));
      loteIds.add(loteId);
    }
    return loteIds;
  }

  private List<List<Camion>> partirCamionesEnLotes(List<Camion> camiones, int cantidadLotes) {
    List<List<Camion>> lotes = new ArrayList<>();
    for (int i = 0; i < cantidadLotes; i++) {
      lotes.add(new ArrayList<>());
    }
    if (cantidadLotes == 0) {
      return lotes;
    }

    for (int i = 0; i < camiones.size(); i++) {
      lotes.get(i % cantidadLotes).add(camiones.get(i));
    }
    return lotes;
  }

  /**
   * Procesa el resultado del planificador para un lote: crea una ruta por camión y notifica las
   * donaciones que pasan a {@code LISTA_PARA_ENTREGAR}.
   *
   * <p>Los destinos de cada camión se respetan en el orden que devolvió el planificador. El
   * resultado se valida completo antes de persistir ninguna ruta: si alguna entrada es inválida, el
   * lote sigue pendiente y no se aplica nada (ni rutas ni notificaciones parciales).
   *
   * @param resultado resultado del lote (no nulo)
   * @throws IllegalStateException si el lote no está pendiente
   * @throws IllegalArgumentException si una donación del resultado no pertenece al lote o no
   *     corresponde a la entidad beneficiaria de su destino
   */
  public void procesarResultado(ResultadoPlanificacion resultado) {
    List<DonacionPlanificable> donacionesDelLote = lotesPendientes.get(resultado.loteId());
    if (donacionesDelLote == null) {
      throw new IllegalStateException("LoteId no encontrado: " + resultado.loteId());
    }

    Map<DonacionId, DonacionPlanificable> porId = indexarPorId(donacionesDelLote);

    List<Ruta> rutas = new ArrayList<>();
    List<List<DonacionId>> donacionesPorRuta = new ArrayList<>();
    for (var entry : resultado.rutas().entrySet()) {
      List<Destino> destinos = entry.getValue();

      validarDestinos(destinos, porId);
      rutas.add(new Ruta(entry.getKey(), destinos));
      donacionesPorRuta.add(donacionesDe(destinos));
    }

    lotesPendientes.remove(resultado.loteId());

    for (int i = 0; i < rutas.size(); i++) {
      rutaRepository.agregar(rutas.get(i));
    }

    for (int i = 0; i < rutas.size(); i++) {
      donacionesRemotas.notificarListaParaEntregar(donacionesPorRuta.get(i));
    }
  }

  private List<List<DonacionPlanificable>> partirEnLotes(
      List<DonacionPlanificable> donaciones, int tamanioMaximo) {
    List<List<DonacionPlanificable>> lotes = new ArrayList<>();
    for (int i = 0; i < donaciones.size(); i += tamanioMaximo) {
      int fin = Math.min(i + tamanioMaximo, donaciones.size());
      lotes.add(donaciones.subList(i, fin));
    }
    return lotes;
  }

  private Map<DonacionId, DonacionPlanificable> indexarPorId(
      List<DonacionPlanificable> donaciones) {
    Map<DonacionId, DonacionPlanificable> porId = new HashMap<>();
    for (DonacionPlanificable donacion : donaciones) {
      porId.put(donacion.idDonacion(), donacion);
    }
    return porId;
  }

  private void validarDestinos(
      List<Destino> destinos, Map<DonacionId, DonacionPlanificable> porId) {
    for (Destino destino : destinos) {
      for (DonacionId id : destino.donaciones()) {
        DonacionPlanificable donacion = porId.get(id);
        if (donacion == null) {
          throw new IllegalArgumentException("la donación planificada no pertenece al lote: " + id);
        }
        if (!donacion.idEntidadBeneficiaria().equals(destino.entidadId())) {
          throw new IllegalArgumentException(
              "la donación no pertenece a la entidad beneficiaria del destino: donacion="
                  + id
                  + ", entidadEsperada="
                  + destino.entidadId()
                  + ", entidadDeLaDonacion="
                  + donacion.idEntidadBeneficiaria());
        }
      }
    }
  }

  private List<DonacionId> donacionesDe(List<Destino> destinos) {
    List<DonacionId> ids = new ArrayList<>();
    for (Destino destino : destinos) {
      ids.addAll(destino.donaciones());
    }
    return ids;
  }
}
