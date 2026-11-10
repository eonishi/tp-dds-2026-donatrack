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

  private ServicioPlanificacionRutas servicio;

  @BeforeEach
  void setUp() {
    servicio =
        new ServicioPlanificacionRutas(
            donacionesRemotas, camionRepository, planificadorRutas, rutaRepository);
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

    List<UUID> resultado = servicio.planificar();

    assertEquals(1, resultado.size());
    assertEquals(loteIdEsperado, resultado.get(0));
    ArgumentCaptor<LoteDonaciones> captor = ArgumentCaptor.forClass(LoteDonaciones.class);
    verify(planificadorRutas, times(1)).enviarLote(captor.capture());
    LoteDonaciones lote = captor.getValue();
    assertEquals(50, lote.donaciones().size());
    assertEquals(1, lote.camiones().size());
    assertSame(camion, lote.camiones().get(0));
  }

  @Test
  void planificar_Con250Donaciones_ParticionaEn3Lotes() {
    Camion camion = crearCamionDummy("ABC123");
    Camion camion2 = crearCamionDummy("DEF456");
    Camion camion3 = crearCamionDummy("GHI789");
    List<DonacionPlanificable> donaciones = new ArrayList<>();
    for (int i = 0; i < 250; i++) {
      donaciones.add(crearPlanificable(1L, nuevoDonacionUuid()));
    }
    when(donacionesRemotas.listarEnAsignacionRealizada()).thenReturn(donaciones);
    when(camionRepository.listarTodos()).thenReturn(List.of(camion, camion2, camion3));
    when(planificadorRutas.enviarLote(any())).thenAnswer(invocacion -> UUID.randomUUID());

    List<UUID> resultado = servicio.planificar();

    assertEquals(3, resultado.size());
    verify(planificadorRutas, times(3)).enviarLote(any());
  }

  @Test
  void planificar_SinDonaciones_NoEnviaNingunLote() {
    when(donacionesRemotas.listarEnAsignacionRealizada()).thenReturn(List.of());
    when(camionRepository.listarTodos()).thenReturn(List.of(crearCamionDummy("ABC123")));

    List<UUID> resultado = servicio.planificar();

    assertTrue(resultado.isEmpty());
    verify(planificadorRutas, never()).enviarLote(any());
  }

  @Test
  void planificar_SinCamiones_NoEnviaNingunLote() {
    when(donacionesRemotas.listarEnAsignacionRealizada())
        .thenReturn(List.of(crearPlanificable(1L, nuevoDonacionUuid())));
    when(camionRepository.listarTodos()).thenReturn(List.of());

    List<UUID> resultado = servicio.planificar();

    assertTrue(resultado.isEmpty());
    verify(planificadorRutas, never()).enviarLote(any());
  }

  @Test
  void planificar_Con250Donaciones_LosLotesNoExceden100Donaciones() {
    Camion camion = crearCamionDummy("ABC123");
    Camion camion2 = crearCamionDummy("DEF456");
    Camion camion3 = crearCamionDummy("GHI789");
    List<DonacionPlanificable> donaciones = new ArrayList<>();
    for (int i = 0; i < 250; i++) {
      donaciones.add(crearPlanificable(1L, nuevoDonacionUuid()));
    }
    when(donacionesRemotas.listarEnAsignacionRealizada()).thenReturn(donaciones);
    when(camionRepository.listarTodos()).thenReturn(List.of(camion, camion2, camion3));
    when(planificadorRutas.enviarLote(any())).thenAnswer(invocacion -> UUID.randomUUID());

    servicio.planificar();

    ArgumentCaptor<LoteDonaciones> captor = ArgumentCaptor.forClass(LoteDonaciones.class);
    verify(planificadorRutas, times(3)).enviarLote(captor.capture());
    for (LoteDonaciones lote : captor.getAllValues()) {
      assertTrue(
          lote.donaciones().size() <= 100, "cada lote debe tener como máximo 100 donaciones");
    }
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
    when(camionRepository.listarTodos()).thenReturn(List.of(camion));
    UUID loteId = UUID.randomUUID();
    when(planificadorRutas.enviarLote(any())).thenReturn(loteId);

    servicio.planificar();

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
    when(camionRepository.listarTodos()).thenReturn(List.of(camion1, camion2));
    UUID loteId = UUID.randomUUID();
    when(planificadorRutas.enviarLote(any())).thenReturn(loteId);

    servicio.planificar();

    ResultadoPlanificacion resultado =
        new ResultadoPlanificacion(
            loteId,
            Map.of(
                camion1, List.of(new Destino(entA, "Dir 1", List.of(d1))),
                camion2, List.of(new Destino(entB, "Dir 2", List.of(d2)))),
            java.util.Collections.emptyList());

    servicio.procesarResultado(resultado);

    verify(rutaRepository, times(2)).agregar(any(Ruta.class));
    verify(donacionesRemotas, times(2)).notificarListaParaEntregar(any());
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
    when(camionRepository.listarTodos()).thenReturn(List.of(camion));
    UUID loteId = UUID.randomUUID();
    when(planificadorRutas.enviarLote(any())).thenReturn(loteId);

    servicio.planificar();

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
    Camion camion = crearCamionDummy("ABC123");
    when(donacionesRemotas.listarEnAsignacionRealizada())
        .thenReturn(List.of(crearPlanificable(1L, nuevoDonacionUuid())));
    when(camionRepository.listarTodos()).thenReturn(List.of(camion));
    UUID loteId = UUID.randomUUID();
    when(planificadorRutas.enviarLote(any())).thenReturn(loteId);

    servicio.planificar();

    servicio.procesarResultado(
        new ResultadoPlanificacion(loteId, Map.of(), java.util.Collections.emptyList()));

    verify(rutaRepository, never()).agregar(any());
    verify(donacionesRemotas, never()).notificarListaParaEntregar(any());
  }

  @Test
  void procesarResultado_LoteDesconocido_DebeLanzarIllegalStateException() {
    ResultadoPlanificacion resultado =
        new ResultadoPlanificacion(UUID.randomUUID(), Map.of(), java.util.Collections.emptyList());

    assertThrows(IllegalStateException.class, () -> servicio.procesarResultado(resultado));
  }

  @Test
  void procesarResultado_DonacionDeOtraEntidad_DebeLanzarIllegalArgumentException() {
    Camion camion = crearCamionDummy("ABC123");
    UUID donacionId = nuevoDonacionUuid();
    Long entidadDelDestino = 2L;

    when(donacionesRemotas.listarEnAsignacionRealizada())
        .thenReturn(List.of(crearPlanificable(1L, donacionId)));
    when(camionRepository.listarTodos()).thenReturn(List.of(camion));
    UUID loteId = UUID.randomUUID();
    when(planificadorRutas.enviarLote(any())).thenReturn(loteId);

    servicio.planificar();

    ResultadoPlanificacion resultado =
        new ResultadoPlanificacion(
            loteId,
            Map.of(camion, List.of(new Destino(entidadDelDestino, "Dir 2", List.of(donacionId)))),
            java.util.Collections.emptyList());

    assertThrows(IllegalArgumentException.class, () -> servicio.procesarResultado(resultado));
    verify(rutaRepository, never()).agregar(any());
  }

  @Test
  void procesarResultado_DonacionFueraDelLote_DebeLanzarIllegalArgumentException() {
    Camion camion = crearCamionDummy("ABC123");
    Long entidad = 1L;
    UUID enElLote = nuevoDonacionUuid();
    UUID ajena = nuevoDonacionUuid();

    when(donacionesRemotas.listarEnAsignacionRealizada())
        .thenReturn(List.of(crearPlanificable(1L, enElLote)));
    when(camionRepository.listarTodos()).thenReturn(List.of(camion));
    UUID loteId = UUID.randomUUID();
    when(planificadorRutas.enviarLote(any())).thenReturn(loteId);

    servicio.planificar();

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
    when(camionRepository.listarTodos()).thenReturn(List.of(camionValido, camionConError));
    UUID loteId = UUID.randomUUID();
    when(planificadorRutas.enviarLote(any())).thenReturn(loteId);

    servicio.planificar();

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
  void procesarResultado_ConEntradaInvalida_ElLoteSiguePendiente() {
    Camion camionValido = crearCamionDummy("ABC123");
    Camion camionConError = crearCamionDummy("XYZ789");
    Long entA = 1L;
    Long entB = 2L;
    UUID deA = nuevoDonacionUuid();
    UUID deB = nuevoDonacionUuid();

    when(donacionesRemotas.listarEnAsignacionRealizada())
        .thenReturn(List.of(crearPlanificable(1L, deA), crearPlanificable(2L, deB)));
    when(camionRepository.listarTodos()).thenReturn(List.of(camionValido, camionConError));
    UUID loteId = UUID.randomUUID();
    when(planificadorRutas.enviarLote(any())).thenReturn(loteId);

    servicio.planificar();

    Map<Camion, List<Destino>> invalido = new LinkedHashMap<>();
    invalido.put(camionValido, List.of(new Destino(entA, "Dir 1", List.of(deA))));
    invalido.put(camionConError, List.of(new Destino(entB, "Dir 2", List.of(deB, deA))));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            servicio.procesarResultado(
                new ResultadoPlanificacion(loteId, invalido, java.util.Collections.emptyList())));

    // El lote no se consumió: el mismo id de lote se puede reprocesar con un resultado válido.
    ResultadoPlanificacion valido =
        new ResultadoPlanificacion(
            loteId,
            Map.of(
                camionValido, List.of(new Destino(entA, "Dir 1", List.of(deA))),
                camionConError, List.of(new Destino(entB, "Dir 2", List.of(deB)))),
            java.util.Collections.emptyList());

    servicio.procesarResultado(valido);

    verify(rutaRepository, times(2)).agregar(any(Ruta.class));
  }
}
