package ar.edu.utn.donatrack.logistica.planificacion;

/*
 * ── LÓGICA FALTANTE — microservicio de DONACIONES ──────────────────────
 * Este servicio orquestaba la planificación de rutas: tomaba las donaciones en
 * estado Asignación Realizada (microservicio de donaciones), las agrupaba en
 * lotes junto con los camiones disponibles, enviaba cada lote al planificador
 * externo y, al recibir el resultado, creaba las rutas y transicionaba el
 * estado de cada donación (asignarCamion/planificarRuta). Todo eso requiere
 * objetos del microservicio de donaciones (Donacion, DonacionesRepository,
 * EntidadBeneficiaria) y no está migrado: al integrar los servicios, este
 * flujo debe reconstruirse mediante llamadas REST al microservicio de
 * donaciones y el callback del planificador externo.
 *
 * El código original queda comentado abajo, sin migrar.
 */
public class ServicioPlanificacionRutas {
  //
  // import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
  // import ar.edu.utn.donatrack.donaciones.Donacion;
  // import ar.edu.utn.donatrack.donaciones.DonacionesRepository;
  // import ar.edu.utn.donatrack.logistica.Camion;
  // import ar.edu.utn.donatrack.logistica.CamionRepository;
  // import ar.edu.utn.donatrack.logistica.Destino;
  // import ar.edu.utn.donatrack.logistica.Ruta;
  // import ar.edu.utn.donatrack.logistica.RutaRepository;
  // import java.util.ArrayList;
  // import java.util.HashMap;
  // import java.util.HashSet;
  // import java.util.List;
  // import java.util.Map;
  // import java.util.Set;
  //
  // public class ServicioPlanificacionRutas {
  //
  //   private final DonacionesRepository donacionesRepository;
  //   private final CamionRepository camionRepository;
  //   private final PlanificadorRutas planificadorRutas;
  //   private final RutaRepository rutaRepository;
  //   private final Map<LoteId, List<Donacion>> lotesPendientes = new HashMap<>();
  //
  //   public ServicioPlanificacionRutas(
  //       DonacionesRepository donacionesRepository,
  //       CamionRepository camionRepository,
  //       PlanificadorRutas planificadorRutas,
  //       RutaRepository rutaRepository) {
  //     this.donacionesRepository = donacionesRepository;
  //     this.camionRepository = camionRepository;
  //     this.planificadorRutas = planificadorRutas;
  //     this.rutaRepository = rutaRepository;
  //   }
  //
  //   public List<LoteId> planificar() {
  //     List<Donacion> disponibles = donacionesRepository.getDonacionesEnAsignacionRealizada();
  //     List<Camion> camiones = camionRepository.listarTodos();
  //
  //     if (disponibles.isEmpty() || camiones.isEmpty()) {
  //       return List.of();
  //     }
  //
  //     List<LoteId> loteIds = new ArrayList<>();
  //     List<List<Donacion>> batches = partirEnLotes(disponibles, 100);
  //
  //     for (List<Donacion> batch : batches) {
  //       LoteDonaciones lote = new LoteDonaciones(batch, camiones);
  //       LoteId loteId = planificadorRutas.enviarLote(lote);
  //       lotesPendientes.put(loteId, new ArrayList<>(batch));
  //       loteIds.add(loteId);
  //     }
  //
  //     return loteIds;
  //   }
  //
  //   public void procesarResultado(ResultadoPlanificacion resultado) {
  //     List<Donacion> batchDonaciones = lotesPendientes.remove(resultado.loteId());
  //     if (batchDonaciones == null) {
  //       throw new IllegalStateException("LoteId no encontrado: " + resultado.loteId());
  //     }
  //
  //     Set<Donacion> planificadas = new HashSet<>();
  //
  //     for (var entry : resultado.rutas().entrySet()) {
  //       Camion camion = entry.getKey();
  //       List<Destino> destinos = new ArrayList<>();
  //
  //       for (EntidadBeneficiaria entidad : entry.getValue()) {
  //         Destino destino = new Destino(entidad);
  //         for (Donacion donacion : batchDonaciones) {
  //           if (entidad.equals(donacion.getEntidadBeneficiaria())) {
  //             destino.agregarDonacion(donacion);
  //             planificadas.add(donacion);
  //           }
  //         }
  //         destinos.add(destino);
  //       }
  //
  //       Ruta ruta = new Ruta(camion, destinos);
  //       rutaRepository.agregar(ruta);
  //
  //       for (Destino destino : destinos) {
  //         for (Donacion donacion : destino.getDonaciones()) {
  //           donacion.asignarCamion(camion);
  //           donacion.planificarRuta();
  //         }
  //       }
  //     }
  //
  //     // Donations not in planificadas remain in AsignacionRealizada (no-op)
  //   }
  //
  //   private List<List<Donacion>> partirEnLotes(List<Donacion> donaciones, int tamanioMaximo) {
  //     List<List<Donacion>> lotes = new ArrayList<>();
  //     for (int i = 0; i < donaciones.size(); i += tamanioMaximo) {
  //       int fin = Math.min(i + tamanioMaximo, donaciones.size());
  //       lotes.add(donaciones.subList(i, fin));
  //     }
  //     return lotes;
  //   }
  // }
  //
}
