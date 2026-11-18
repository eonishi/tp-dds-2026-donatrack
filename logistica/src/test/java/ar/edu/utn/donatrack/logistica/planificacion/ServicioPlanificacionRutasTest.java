package ar.edu.utn.donatrack.logistica.planificacion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ar.edu.utn.donatrack.logistica.Camion;
import ar.edu.utn.donatrack.logistica.CamionRepository;
import ar.edu.utn.donatrack.logistica.Destino;
import ar.edu.utn.donatrack.logistica.Ruta;
import ar.edu.utn.donatrack.logistica.RutaRepository;
import ar.edu.utn.donatrack.logistica.monitoreo.Coordenada;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServicioPlanificacionRutasTest {

  @Mock DonacionesRemotas donacionesRemotas;
  @Mock CamionRepository camionRepository;
  @Mock PlanificadorRutas planificadorRutas;
  @Mock RutaRepository rutaRepository;

  private static final String URL_CALLBACK =
      "http://logistica.test/logistica/planificacion/callback";

  private ServicioPlanificacionRutas servicio;

  @BeforeEach
  void setUp() {
    servicio =
        new ServicioPlanificacionRutas(
            donacionesRemotas, camionRepository, planificadorRutas, rutaRepository, URL_CALLBACK);
  }

  // ── Helpers ────────────────────────────────────────────────────────

  private static Camion crearCamionDummy(String patente) {
    return new Camion(patente, 45.0, 2.5, 5000.0);
  }

  private static DonacionPlanificable crearPlanificable(long entidadId, UUID donacionId) {
    return new DonacionPlanificable(
        donacionId, entidadId, "Dir " + entidadId, new Coordenada(-34.6, -58.4), 1.0, 1.0);
  }

  private static UUID nuevoDonacionUuid() {
    return UUID.randomUUID();
  }

  // ── planificar() ───────────────────────────────────────────────────

  @Test
  void planificar_Con50DonacionesUnCamion_EnviaUnLote() {
    Camion camion = crearCamionDummy("ABC123");
    List<DonacionPlanificable> donaciones = new ArrayList<>();
    for (int i = 0; i < 50; i++) {
      donaciones.add(crearPlanificable(1L, nuevoDonacionUuid()));
    }
    when(donacionesRemotas.listarEnAsignacionRealizada()).thenReturn(donaciones);
    when(camionRepository.listarTodos()).thenReturn(List.of(camion));
    UUID loteIdEsperado = UUID.randomUUID();
    when(planificadorRutas.enviarLote(any())).thenReturn(loteIdEsperado);

    Optional<UUID> resultado = servicio.planificar();

    assertEquals(Optional.of(loteIdEsperado), resultado);
    ArgumentCaptor<LoteDonaciones> captor = ArgumentCaptor.forClass(LoteDonaciones.class);
    verify(planificadorRutas, times(1)).enviarLote(captor.capture());
    LoteDonaciones lote = captor.getValue();
    assertEquals(50, lote.donaciones().size());
    assertEquals(1, lote.camiones().size());
    assertSame(camion, lote.camiones().get(0));
  }

  @Test
  void planificar_Con250Donaciones_EnviaUnSoloLoteConLasPrimeras100YTodosLosCamiones() {
    List<Camion> camiones =
        List.of(crearCamionDummy("ABC123"), crearCamionDummy("DEF456"), crearCamionDummy("GHI789"));
    List<DonacionPlanificable> donaciones = new ArrayList<>();
    for (int i = 0; i < 250; i++) {
      donaciones.add(crearPlanificable(1L, nuevoDonacionUuid()));
    }
    when(donacionesRemotas.listarEnAsignacionRealizada()).thenReturn(donaciones);
    when(camionRepository.listarTodos()).thenReturn(camiones);
    UUID loteIdEsperado = UUID.randomUUID();
    when(planificadorRutas.enviarLote(any())).thenReturn(loteIdEsperado);

    Optional<UUID> resultado = servicio.planificar();

    assertEquals(Optional.of(loteIdEsperado), resultado);
    ArgumentCaptor<LoteDonaciones> captor = ArgumentCaptor.forClass(LoteDonaciones.class);
    verify(planificadorRutas, times(1)).enviarLote(captor.capture());
    LoteDonaciones lote = captor.getValue();
    assertEquals(donaciones.subList(0, 100), lote.donaciones());
    assertEquals(camiones, lote.camiones());
  }

  @Test
  void planificar_SinDonaciones_NoEnviaNingunLote() {
    when(donacionesRemotas.listarEnAsignacionRealizada()).thenReturn(List.of());
    when(camionRepository.listarTodos()).thenReturn(List.of(crearCamionDummy("ABC123")));

    Optional<UUID> resultado = servicio.planificar();

    assertTrue(resultado.isEmpty());
    verify(planificadorRutas, never()).enviarLote(any());
  }

  @Test
  void planificar_SinCamiones_NoEnviaNingunLote() {
    when(donacionesRemotas.listarEnAsignacionRealizada())
        .thenReturn(List.of(crearPlanificable(1L, nuevoDonacionUuid())));
    when(camionRepository.listarTodos()).thenReturn(List.of());

    Optional<UUID> resultado = servicio.planificar();

    assertTrue(resultado.isEmpty());
    verify(planificadorRutas, never()).enviarLote(any());
  }

  // ── procesarResultado() ────────────────────────────────────────────

  @Test
  void procesarResultado_UnCamionTresEntidades_CreaRutaConDestinosOrdenados() {
    Camion camion = crearCamionDummy("ABC123");
    Long entA = 1L;
    Long entB = 2L;
    Long entC = 3L;
    UUID dA1 = nuevoDonacionUuid();
    UUID dA2 = nuevoDonacionUuid();
    UUID dB1 = nuevoDonacionUuid();
    UUID dB2 = nuevoDonacionUuid();
    UUID dC1 = nuevoDonacionUuid();
    UUID dC2 = nuevoDonacionUuid();

    List<DonacionPlanificable> todas =
        List.of(
            crearPlanificable(1L, dA1),
            crearPlanificable(1L, dA2),
            crearPlanificable(2L, dB1),
            crearPlanificable(2L, dB2),
            crearPlanificable(3L, dC1),
            crearPlanificable(3L, dC2));
    when(donacionesRemotas.listarEnAsignacionRealizada()).thenReturn(todas);
    UUID loteId = UUID.randomUUID();

    // El planificador devuelve el orden [B, A, C]
    ResultadoPlanificacion resultado =
        new ResultadoPlanificacion(
            loteId,
            Map.of(
                camion,
                List.of(
                    new Destino(entB, "Dir 2", List.of(dB1, dB2)),
                    new Destino(entA, "Dir 1", List.of(dA1, dA2)),
                    new Destino(entC, "Dir 3", List.of(dC1, dC2)))),
            java.util.Collections.emptyList());

    servicio.procesarResultado(resultado);

    ArgumentCaptor<Ruta> rutaCaptor = ArgumentCaptor.forClass(Ruta.class);
    verify(rutaRepository, times(1)).agregar(rutaCaptor.capture());
    Ruta ruta = rutaCaptor.getValue();
    assertSame(camion, ruta.getCamion());
    assertEquals(3, ruta.getDestinos().size());
    assertEquals(entB, ruta.getDestinos().get(0).entidadId());
    assertEquals(entA, ruta.getDestinos().get(1).entidadId());
    assertEquals(entC, ruta.getDestinos().get(2).entidadId());
    assertEquals(2, ruta.getDestinos().get(0).donaciones().size());
    assertEquals(2, ruta.getDestinos().get(1).donaciones().size());
    assertEquals(2, ruta.getDestinos().get(2).donaciones().size());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<UUID>> notificadas = ArgumentCaptor.forClass(List.class);
    verify(donacionesRemotas, times(1)).notificarListaParaEntregar(notificadas.capture());
    assertEquals(List.of(dB1, dB2, dA1, dA2, dC1, dC2), notificadas.getValue());
  }

  @Test
  void procesarResultado_MultiplesCamiones_CreaMultiplesRutas() {
    Camion camion1 = crearCamionDummy("ABC123");
    Camion camion2 = crearCamionDummy("XYZ789");
    Long entA = 1L;
    Long entB = 2L;
    UUID d1 = nuevoDonacionUuid();
    UUID d2 = nuevoDonacionUuid();

    List<DonacionPlanificable> todas =
        List.of(crearPlanificable(1L, d1), crearPlanificable(2L, d2));
    when(donacionesRemotas.listarEnAsignacionRealizada()).thenReturn(todas);
    UUID loteId = UUID.randomUUID();

    ResultadoPlanificacion resultado =
        new ResultadoPlanificacion(
            loteId,
            Map.of(
                camion1, List.of(new Destino(entA, "Dir 1", List.of(d1))),
                camion2, List.of(new Destino(entB, "Dir 2", List.of(d2)))),
            java.util.Collections.emptyList());

    servicio.procesarResultado(resultado);

    verify(rutaRepository, times(2)).agregar(any(Ruta.class));
    // Un solo aviso con todas las donaciones: donaciones aplica el cambio de forma atómica.
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<UUID>> notificadas = ArgumentCaptor.forClass(List.class);
    verify(donacionesRemotas, times(1)).notificarListaParaEntregar(notificadas.capture());
    assertEquals(2, notificadas.getValue().size());
    assertTrue(notificadas.getValue().containsAll(List.of(d1, d2)));
  }

  @Test
  void procesarResultado_DonacionNoIncluidaEnElResultado_NoSeNotifica() {
    Camion camion = crearCamionDummy("ABC123");
    Long entA = 1L;
    UUID planificada = nuevoDonacionUuid();
    UUID noPlanificada = nuevoDonacionUuid();

    List<DonacionPlanificable> todas =
        List.of(crearPlanificable(1L, planificada), crearPlanificable(2L, noPlanificada));
    when(donacionesRemotas.listarEnAsignacionRealizada()).thenReturn(todas);
    UUID loteId = UUID.randomUUID();

    // El resultado solo incluye la donación de la entidad A
    ResultadoPlanificacion resultado =
        new ResultadoPlanificacion(
            loteId,
            Map.of(camion, List.of(new Destino(entA, "Dir 1", List.of(planificada)))),
            java.util.Collections.emptyList());

    servicio.procesarResultado(resultado);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<UUID>> notificadas = ArgumentCaptor.forClass(List.class);
    verify(donacionesRemotas, times(1)).notificarListaParaEntregar(notificadas.capture());
    assertEquals(List.of(planificada), notificadas.getValue());
  }

  @Test
  void procesarResultado_ResultadoVacio_NoCreaRutas() {
    UUID loteId = UUID.randomUUID();

    servicio.procesarResultado(
        new ResultadoPlanificacion(loteId, Map.of(), java.util.Collections.emptyList()));

    verify(rutaRepository, never()).agregar(any());
    verify(donacionesRemotas, never()).notificarListaParaEntregar(any());
  }

  @Test
  void procesarResultado_DonacionDeOtraEntidad_DebeLanzarIllegalArgumentException() {
    Camion camion = crearCamionDummy("ABC123");
    UUID donacionId = nuevoDonacionUuid();
    Long entidadDelDestino = 2L;

    when(donacionesRemotas.listarEnAsignacionRealizada())
        .thenReturn(List.of(crearPlanificable(1L, donacionId)));
    UUID loteId = UUID.randomUUID();

    ResultadoPlanificacion resultado =
        new ResultadoPlanificacion(
            loteId,
            Map.of(camion, List.of(new Destino(entidadDelDestino, "Dir 2", List.of(donacionId)))),
            java.util.Collections.emptyList());

    assertThrows(IllegalArgumentException.class, () -> servicio.procesarResultado(resultado));
    verify(rutaRepository, never()).agregar(any());
  }

  @Test
  void
      procesarResultado_DonacionQueNoEstaEnAsignacionRealizada_DebeLanzarIllegalArgumentException() {
    Camion camion = crearCamionDummy("ABC123");
    Long entidad = 1L;
    UUID enAsignacionRealizada = nuevoDonacionUuid();
    UUID ajena = nuevoDonacionUuid();

    when(donacionesRemotas.listarEnAsignacionRealizada())
        .thenReturn(List.of(crearPlanificable(1L, enAsignacionRealizada)));
    UUID loteId = UUID.randomUUID();

    ResultadoPlanificacion resultado =
        new ResultadoPlanificacion(
            loteId,
            Map.of(camion, List.of(new Destino(entidad, "Dir 1", List.of(ajena)))),
            java.util.Collections.emptyList());

    assertThrows(IllegalArgumentException.class, () -> servicio.procesarResultado(resultado));
    verify(rutaRepository, never()).agregar(any());
  }

  @Test
  void procesarResultado_ConEntradaInvalidaEnElSegundoCamion_NoAplicaNingunaRuta() {
    Camion camionValido = crearCamionDummy("ABC123");
    Camion camionConError = crearCamionDummy("XYZ789");
    Long entA = 1L;
    Long entB = 2L;
    UUID deA = nuevoDonacionUuid();
    UUID deB = nuevoDonacionUuid();

    when(donacionesRemotas.listarEnAsignacionRealizada())
        .thenReturn(List.of(crearPlanificable(1L, deA), crearPlanificable(2L, deB)));
    UUID loteId = UUID.randomUUID();

    // La primera entrada es válida y la segunda inválida (deA no pertenece a entB). El orden se
    // fija con LinkedHashMap para que la entrada inválida se itere última.
    Map<Camion, List<Destino>> rutas = new LinkedHashMap<>();
    rutas.put(camionValido, List.of(new Destino(entA, "Dir 1", List.of(deA))));
    rutas.put(camionConError, List.of(new Destino(entB, "Dir 2", List.of(deA))));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            servicio.procesarResultado(
                new ResultadoPlanificacion(loteId, rutas, java.util.Collections.emptyList())));

    verify(rutaRepository, never()).agregar(any());
    verify(donacionesRemotas, never()).notificarListaParaEntregar(any());
  }

  @Test
  void procesarResultado_DonacionRepetidaEnElResultado_DebeLanzarIllegalArgumentException() {
    Camion camion1 = crearCamionDummy("ABC123");
    Camion camion2 = crearCamionDummy("XYZ789");
    UUID donacion = nuevoDonacionUuid();
    when(donacionesRemotas.listarEnAsignacionRealizada())
        .thenReturn(List.of(crearPlanificable(1L, donacion)));

    Map<Camion, List<Destino>> rutas = new LinkedHashMap<>();
    rutas.put(camion1, List.of(new Destino(1L, "Dir 1", List.of(donacion))));
    rutas.put(camion2, List.of(new Destino(1L, "Dir 1", List.of(donacion))));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            servicio.procesarResultado(
                new ResultadoPlanificacion(
                    UUID.randomUUID(), rutas, java.util.Collections.emptyList())));
    verify(rutaRepository, never()).agregar(any());
    verify(donacionesRemotas, never()).notificarListaParaEntregar(any());
  }

  @Test
  void procesarResultado_NotificaADonacionesAntesDeGuardarLasRutas() {
    Camion camion = crearCamionDummy("ABC123");
    UUID donacion = nuevoDonacionUuid();
    when(donacionesRemotas.listarEnAsignacionRealizada())
        .thenReturn(List.of(crearPlanificable(1L, donacion)));

    servicio.procesarResultado(
        new ResultadoPlanificacion(
            UUID.randomUUID(),
            Map.of(camion, List.of(new Destino(1L, "Dir 1", List.of(donacion)))),
            java.util.Collections.emptyList()));

    var orden = inOrder(donacionesRemotas, rutaRepository);
    orden.verify(donacionesRemotas).notificarListaParaEntregar(List.of(donacion));
    orden.verify(rutaRepository).agregar(any(Ruta.class));
  }

  @Test
  void procesarResultado_SiFallaElAvisoADonaciones_NoGuardaRutas() {
    Camion camion = crearCamionDummy("ABC123");
    UUID donacion = nuevoDonacionUuid();
    when(donacionesRemotas.listarEnAsignacionRealizada())
        .thenReturn(List.of(crearPlanificable(1L, donacion)));
    doThrow(new RuntimeException("Error HTTP 400"))
        .when(donacionesRemotas)
        .notificarListaParaEntregar(any());

    assertThrows(
        RuntimeException.class,
        () ->
            servicio.procesarResultado(
                new ResultadoPlanificacion(
                    UUID.randomUUID(),
                    Map.of(camion, List.of(new Destino(1L, "Dir 1", List.of(donacion)))),
                    java.util.Collections.emptyList())));
    verify(rutaRepository, never()).agregar(any());
  }

  @Test
  void planificar_ElLoteLlevaLaUrlDeCallback() {
    when(donacionesRemotas.listarEnAsignacionRealizada())
        .thenReturn(List.of(crearPlanificable(1L, nuevoDonacionUuid())));
    when(camionRepository.listarTodos()).thenReturn(List.of(crearCamionDummy("ABC123")));
    when(planificadorRutas.enviarLote(any())).thenReturn(UUID.randomUUID());

    servicio.planificar();

    ArgumentCaptor<LoteDonaciones> captor = ArgumentCaptor.forClass(LoteDonaciones.class);
    verify(planificadorRutas).enviarLote(captor.capture());
    assertEquals(URL_CALLBACK, captor.getValue().urlCallback());
  }

  @Test
  void procesarResultado_DonacionesRemotasConIdRepetido_FallaSinAplicarNada() {
    Camion camion = crearCamionDummy("ABC123");
    UUID donacion = nuevoDonacionUuid();
    when(donacionesRemotas.listarEnAsignacionRealizada())
        .thenReturn(List.of(crearPlanificable(1L, donacion), crearPlanificable(1L, donacion)));

    assertThrows(
        IllegalStateException.class,
        () ->
            servicio.procesarResultado(
                new ResultadoPlanificacion(
                    UUID.randomUUID(),
                    Map.of(camion, List.of(new Destino(1L, "Dir 1", List.of(donacion)))),
                    java.util.Collections.emptyList())));
    verify(donacionesRemotas, never()).notificarListaParaEntregar(any());
    verify(rutaRepository, never()).agregar(any());
  }

  @Test
  void procesarResultado_CamionSinDestinos_NoCreaRuta() {
    Camion conDestinos = crearCamionDummy("ABC123");
    Camion sinDestinos = crearCamionDummy("XYZ789");
    UUID donacion = nuevoDonacionUuid();
    when(donacionesRemotas.listarEnAsignacionRealizada())
        .thenReturn(List.of(crearPlanificable(1L, donacion)));

    Map<Camion, List<Destino>> rutas = new LinkedHashMap<>();
    rutas.put(conDestinos, List.of(new Destino(1L, "Dir 1", List.of(donacion))));
    rutas.put(sinDestinos, List.of());

    servicio.procesarResultado(
        new ResultadoPlanificacion(UUID.randomUUID(), rutas, java.util.Collections.emptyList()));

    ArgumentCaptor<Ruta> rutaCaptor = ArgumentCaptor.forClass(Ruta.class);
    verify(rutaRepository, times(1)).agregar(rutaCaptor.capture());
    assertSame(conDestinos, rutaCaptor.getValue().getCamion());
  }

  @Test
  void procesarResultado_TodosLosCamionesSinDestinos_NoHaceNada() {
    servicio.procesarResultado(
        new ResultadoPlanificacion(
            UUID.randomUUID(),
            Map.of(crearCamionDummy("ABC123"), List.of()),
            java.util.Collections.emptyList()));

    verify(rutaRepository, never()).agregar(any());
    verify(donacionesRemotas, never()).listarEnAsignacionRealizada();
    verify(donacionesRemotas, never()).notificarListaParaEntregar(any());
  }
}
