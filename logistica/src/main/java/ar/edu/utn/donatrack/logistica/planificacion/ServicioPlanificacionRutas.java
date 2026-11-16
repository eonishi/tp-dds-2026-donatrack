package ar.edu.utn.donatrack.logistica.planificacion;

import ar.edu.utn.donatrack.logistica.Camion;
import ar.edu.utn.donatrack.logistica.CamionRepository;
import ar.edu.utn.donatrack.logistica.Destino;
import ar.edu.utn.donatrack.logistica.Ruta;
import ar.edu.utn.donatrack.logistica.RutaRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orquesta la planificación de rutas.
 *
 * <p>Toma las donaciones en estado {@code ASIGNACION_REALIZADA} a través de {@link
 * DonacionesRemotas}, envía al planificador externo un único lote con hasta {@value
 * LoteDonaciones#MAXIMO_DONACIONES} donaciones junto con todos los camiones disponibles y, cuando
 * llega el resultado, materializa las {@link Ruta} y avisa al microservicio de donaciones qué
 * donaciones pasaron a {@code LISTA_PARA_ENTREGAR}.
 *
 * <p>Los lotes no se guardan: el envío lo hace el runner y el resultado llega al callback, que
 * corre en otro proceso. El resultado se valida contra las donaciones que siguen en {@code
 * ASIGNACION_REALIZADA}. Las donaciones que exceden el tope y las que el planificador no asignó
 * quedan en ese estado y entran en la próxima corrida del runner.
 */
public class ServicioPlanificacionRutas {

  private final DonacionesRemotas donacionesRemotas;
  private final CamionRepository camionRepository;
  private final PlanificadorRutas planificadorRutas;
  private final RutaRepository rutaRepository;
  private final String urlCallback;

  /**
   * Crea el servicio de planificación.
   *
   * @param donacionesRemotas puerto hacia el microservicio de donaciones (no nulo)
   * @param camionRepository repositorio de camiones disponibles (no nulo)
   * @param planificadorRutas planificador externo de rutas (no nulo)
   * @param rutaRepository repositorio donde se guardan las rutas armadas (no nulo)
   * @param urlCallback URL del callback que se le pasa al planificador en cada lote (no nula)
   */
  public ServicioPlanificacionRutas(
      DonacionesRemotas donacionesRemotas,
      CamionRepository camionRepository,
      PlanificadorRutas planificadorRutas,
      RutaRepository rutaRepository,
      String urlCallback) {
    this.donacionesRemotas =
        Objects.requireNonNull(donacionesRemotas, "donacionesRemotas no debe ser null");
    this.camionRepository =
        Objects.requireNonNull(camionRepository, "camionRepository no debe ser null");
    this.planificadorRutas =
        Objects.requireNonNull(planificadorRutas, "planificadorRutas no debe ser null");
    this.rutaRepository = Objects.requireNonNull(rutaRepository, "rutaRepository no debe ser null");
    this.urlCallback = Objects.requireNonNull(urlCallback, "urlCallback no debe ser null");
  }

  /**
   * Envía al planificador externo un único lote con las primeras {@value
   * LoteDonaciones#MAXIMO_DONACIONES} donaciones pendientes y todos los camiones.
   *
   * <p>Se envía un solo lote por corrida para que ningún camión reciba dos rutas el mismo día. Las
   * donaciones que no entran quedan en {@code ASIGNACION_REALIZADA} para la próxima corrida.
   *
   * @return el identificador del lote enviado, o vacío si no hay donaciones o camiones
   */
  public Optional<UUID> planificar() {
    List<DonacionPlanificable> disponibles = donacionesRemotas.listarEnAsignacionRealizada();
    List<Camion> camiones = camionRepository.listarTodos();

    if (disponibles.isEmpty() || camiones.isEmpty()) {
      return Optional.empty();
    }

    List<DonacionPlanificable> primeras =
        disponibles.subList(0, Math.min(disponibles.size(), LoteDonaciones.MAXIMO_DONACIONES));
    return Optional.of(
        planificadorRutas.enviarLote(new LoteDonaciones(primeras, camiones, urlCallback)));
  }

  /**
   * Procesa el resultado del planificador para un lote: crea una ruta por camión y notifica las
   * donaciones que pasan a {@code LISTA_PARA_ENTREGAR}.
   *
   * <p>Los destinos de cada camión se respetan en el orden que devolvió el planificador. El
   * resultado se valida completo antes de aplicar nada: cada donación tiene que seguir en {@code
   * ASIGNACION_REALIZADA}, aparecer una sola vez y corresponder a la entidad beneficiaria de su
   * destino. Un resultado repetido falla en esa validación, porque sus donaciones ya cambiaron de
   * estado.
   *
   * <p>Primero se avisa a donaciones con un único pedido, que aplica el cambio de forma atómica, y
   * recién después se guardan las rutas: si el aviso falla, no queda ninguna ruta. Las donaciones
   * no asignadas y los camiones sin destinos no se tocan.
   *
   * @param resultado resultado del lote (no nulo)
   * @throws IllegalArgumentException si una donación del resultado no está en {@code
   *     ASIGNACION_REALIZADA}, está repetida o no corresponde a la entidad beneficiaria de su
   *     destino
   * @throws IllegalStateException si el microservicio de donaciones devuelve dos donaciones con el
   *     mismo id
   */
  public void procesarResultado(ResultadoPlanificacion resultado) {
    // Un camión sin destinos no tiene ruta: si se guardara, un resultado repetido la duplicaría.
    List<Map.Entry<Camion, List<Destino>>> camionesConDestinos =
        resultado.rutas().entrySet().stream().filter(entry -> !entry.getValue().isEmpty()).toList();
    if (camionesConDestinos.isEmpty()) {
      return;
    }

    Map<UUID, DonacionPlanificable> mapDonaciones =
        donacionesRemotas.listarEnAsignacionRealizada().stream()
            .collect(Collectors.toMap(DonacionPlanificable::idDonacion, Function.identity()));

    List<Ruta> rutas = new ArrayList<>();
    List<UUID> planificadas = new ArrayList<>();
    // Se comparte entre camiones: una donación no puede aparecer en dos rutas del mismo resultado.
    Set<UUID> vistas = new HashSet<>();

    // Valido la integridad de los datos devueltos por el planificador
    for (var camionConDestinos : camionesConDestinos) {
      List<Destino> destinos = camionConDestinos.getValue();

      validarDestinos(destinos, mapDonaciones, vistas);
      rutas.add(new Ruta(camionConDestinos.getKey(), destinos));
      planificadas.addAll(donacionesDe(destinos));
    }

    if (!planificadas.isEmpty()) {
      donacionesRemotas.notificarListaParaEntregar(planificadas);
    }

    for (Ruta ruta : rutas) {
      rutaRepository.agregar(ruta);
    }
  }

  private void validarDestinos(
      List<Destino> destinos, Map<UUID, DonacionPlanificable> mapDonaciones, Set<UUID> vistas) {
    for (Destino destino : destinos) {
      for (UUID id : destino.donaciones()) {
        // Por cada donación de cada destino
        DonacionPlanificable donacion = mapDonaciones.get(id);
        if (donacion == null) {
          throw new IllegalArgumentException(
              "la donación planificada no está en ASIGNACION_REALIZADA: %s".formatted(id));
        }
        if (!vistas.add(id)) {
          throw new IllegalArgumentException(
              "la donación aparece más de una vez en el resultado: %s".formatted(id));
        }
        if (!donacion.idEntidadBeneficiaria().equals(destino.entidadId())) {
          throw new IllegalArgumentException(
              "la donación no pertenece a la entidad beneficiaria del destino: "
                  + "donacion=%s, entidadEsperada=%s, entidadDeLaDonacion=%s"
                      .formatted(id, destino.entidadId(), donacion.idEntidadBeneficiaria()));
        }
      }
    }
  }

  private List<UUID> donacionesDe(List<Destino> destinos) {
    List<UUID> ids = new ArrayList<>();
    for (Destino destino : destinos) {
      ids.addAll(destino.donaciones());
    }
    return ids;
  }
}
