package ar.edu.utn.donatrack.planificacion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.bienes.Categoria;
import ar.edu.utn.donatrack.bienes.EstadoUso;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.bienes.UnidadMedida;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.DonacionesRepository;
import ar.edu.utn.donatrack.donaciones.EstadoDonacion;
import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.Genero;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.TipoDocumento;
import ar.edu.utn.donatrack.logistica.Camion;
import ar.edu.utn.donatrack.logistica.CamionRepository;
import ar.edu.utn.donatrack.logistica.Ruta;
import ar.edu.utn.donatrack.logistica.RutaRepository;
import ar.edu.utn.donatrack.logistica.planificacion.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServicioPlanificacionRutasTest {

  @Mock DonacionesRepository donacionesRepository;
  @Mock CamionRepository camionRepository;
  @Mock PlanificadorRutas planificadorRutas;
  @Mock RutaRepository rutaRepository;

  private ServicioPlanificacionRutas servicio;

  private static Donante crearDonanteDummy() {
    return new PersonaHumana(
        new CorreoElectronico("test@test.com"),
        "Juan",
        "Perez",
        30,
        "12345678",
        TipoDocumento.DNI,
        Genero.MASCULINO,
        "Calle Falsa 123");
  }

  private static Bien crearBienDummy() {
    return new Bien(
        "Test",
        "",
        1.0,
        UnidadMedida.UNIDADES,
        null,
        EstadoUso.NO_APLICA,
        new SubCategoria("X", Categoria.ALIMENTO));
  }

  private static Camion crearCamionDummy(String patente) {
    return new Camion(patente, 45.0, 2.5, 5000.0);
  }

  private static EntidadBeneficiaria crearEntidad(String razonSocial) {
    return new EntidadBeneficiaria(razonSocial, "Dir " + razonSocial, "Tel");
  }

  private Donacion crearDonacion(EstadoDonacion estado, EntidadBeneficiaria entidad) {
    Donacion donacion = new Donacion(crearDonanteDummy(), List.of(crearBienDummy()));
    // Walk through state machine to reach desired state
    if (estado == EstadoDonacion.EN_DEPOSITO) {
      return donacion;
    }
    if (estado == EstadoDonacion.ASIGNACION_REALIZADA) {
      donacion.confirmarEntidadBeneficiaria(entidad);
      return donacion;
    }
    throw new IllegalArgumentException("Estado no soportado: " + estado);
  }

  @BeforeEach
  void setUp() {
    servicio =
        new ServicioPlanificacionRutas(
            donacionesRepository, camionRepository, planificadorRutas, rutaRepository);
  }

  // ── planificar() tests ─────────────────────────────────────────────

  @Test
  void planificar_Con50DonacionesUnCamion_EnviaUnLote() {
    Camion camion = crearCamionDummy("ABC123");
    EntidadBeneficiaria entidad = crearEntidad("Entidad A");
    List<Donacion> donaciones = new ArrayList<>();
    for (int i = 0; i < 50; i++) {
      donaciones.add(crearDonacion(EstadoDonacion.ASIGNACION_REALIZADA, entidad));
    }
    when(donacionesRepository.getDonacionesEnAsignacionRealizada()).thenReturn(donaciones);
    when(camionRepository.listarTodos()).thenReturn(List.of(camion));
    LoteId loteIdEsperado = LoteId.generar();
    when(planificadorRutas.enviarLote(any())).thenReturn(loteIdEsperado);

    List<LoteId> resultado = servicio.planificar();

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
    EntidadBeneficiaria entidad = crearEntidad("Entidad A");
    List<Donacion> donaciones = new ArrayList<>();
    for (int i = 0; i < 250; i++) {
      donaciones.add(crearDonacion(EstadoDonacion.ASIGNACION_REALIZADA, entidad));
    }
    when(donacionesRepository.getDonacionesEnAsignacionRealizada()).thenReturn(donaciones);
    when(camionRepository.listarTodos()).thenReturn(List.of(camion));
    when(planificadorRutas.enviarLote(any())).thenReturn(LoteId.generar());

    List<LoteId> resultado = servicio.planificar();

    assertEquals(3, resultado.size());
    verify(planificadorRutas, times(3)).enviarLote(any());
  }

  @Test
  void planificar_SinDonaciones_NoEnviaNingunLote() {
    when(donacionesRepository.getDonacionesEnAsignacionRealizada()).thenReturn(List.of());
    when(camionRepository.listarTodos()).thenReturn(List.of(crearCamionDummy("ABC123")));

    List<LoteId> resultado = servicio.planificar();

    assertTrue(resultado.isEmpty());
    verify(planificadorRutas, never()).enviarLote(any());
  }

  @Test
  void planificar_SinCamiones_NoEnviaNingunLote() {
    EntidadBeneficiaria entidad = crearEntidad("Entidad A");
    List<Donacion> donaciones = new ArrayList<>();
    donaciones.add(crearDonacion(EstadoDonacion.ASIGNACION_REALIZADA, entidad));
    when(donacionesRepository.getDonacionesEnAsignacionRealizada()).thenReturn(donaciones);
    when(camionRepository.listarTodos()).thenReturn(List.of());

    List<LoteId> resultado = servicio.planificar();

    assertTrue(resultado.isEmpty());
    verify(planificadorRutas, never()).enviarLote(any());
  }

  // ── procesarResultado() tests ────────────────────────────────────────

  @Test
  void procesarResultado_UnCamionTresEntidades_CreaRutaConDestinosOrdenados() {
    Camion camion = crearCamionDummy("ABC123");
    EntidadBeneficiaria entA = crearEntidad("Entidad A");
    EntidadBeneficiaria entB = crearEntidad("Entidad B");
    EntidadBeneficiaria entC = crearEntidad("Entidad C");

    // 2 donations per entity, 6 total
    Donacion dA1 = crearDonacion(EstadoDonacion.ASIGNACION_REALIZADA, entA);
    Donacion dA2 = crearDonacion(EstadoDonacion.ASIGNACION_REALIZADA, entA);
    Donacion dB1 = crearDonacion(EstadoDonacion.ASIGNACION_REALIZADA, entB);
    Donacion dB2 = crearDonacion(EstadoDonacion.ASIGNACION_REALIZADA, entB);
    Donacion dC1 = crearDonacion(EstadoDonacion.ASIGNACION_REALIZADA, entC);
    Donacion dC2 = crearDonacion(EstadoDonacion.ASIGNACION_REALIZADA, entC);
    List<Donacion> todas = List.of(dA1, dA2, dB1, dB2, dC1, dC2);

    when(donacionesRepository.getDonacionesEnAsignacionRealizada()).thenReturn(todas);
    when(camionRepository.listarTodos()).thenReturn(List.of(camion));
    LoteId loteId = LoteId.generar();
    when(planificadorRutas.enviarLote(any())).thenReturn(loteId);

    servicio.planificar();

    // Planner returns order [B, A, C]
    ResultadoPlanificacion resultado =
        new ResultadoPlanificacion(loteId, Map.of(camion, List.of(entB, entA, entC)));

    servicio.procesarResultado(resultado);

    // Verify one Ruta created
    ArgumentCaptor<Ruta> rutaCaptor = ArgumentCaptor.forClass(Ruta.class);
    verify(rutaRepository, times(1)).agregar(rutaCaptor.capture());
    Ruta ruta = rutaCaptor.getValue();
    assertSame(camion, ruta.getCamion());
    assertEquals(3, ruta.getDestinos().size());

    // Verify order: B, A, C (per planner)
    assertEquals(entB, ruta.getDestinos().get(0).getEntidadBeneficiaria());
    assertEquals(entA, ruta.getDestinos().get(1).getEntidadBeneficiaria());
    assertEquals(entC, ruta.getDestinos().get(2).getEntidadBeneficiaria());

    // Each destino has 2 donations
    assertEquals(2, ruta.getDestinos().get(0).getDonaciones().size());
    assertEquals(2, ruta.getDestinos().get(1).getDonaciones().size());
    assertEquals(2, ruta.getDestinos().get(2).getDonaciones().size());

    // All donations transitioned to ListaParaEntregar
    for (Donacion d : todas) {
      assertEquals(EstadoDonacion.LISTA_PARA_ENTREGAR, d.getEstado());
    }
  }

  @Test
  void procesarResultado_MultiplesCamiones_CreaMultiplesRutas() {
    Camion camion1 = crearCamionDummy("ABC123");
    Camion camion2 = crearCamionDummy("XYZ789");
    EntidadBeneficiaria entA = crearEntidad("Entidad A");
    EntidadBeneficiaria entB = crearEntidad("Entidad B");

    Donacion d1 = crearDonacion(EstadoDonacion.ASIGNACION_REALIZADA, entA);
    Donacion d2 = crearDonacion(EstadoDonacion.ASIGNACION_REALIZADA, entB);
    List<Donacion> todas = List.of(d1, d2);

    when(donacionesRepository.getDonacionesEnAsignacionRealizada()).thenReturn(todas);
    when(camionRepository.listarTodos()).thenReturn(List.of(camion1, camion2));
    LoteId loteId = LoteId.generar();
    when(planificadorRutas.enviarLote(any())).thenReturn(loteId);

    servicio.planificar();

    ResultadoPlanificacion resultado =
        new ResultadoPlanificacion(
            loteId,
            Map.of(
                camion1, List.of(entA),
                camion2, List.of(entB)));

    servicio.procesarResultado(resultado);

    verify(rutaRepository, times(2)).agregar(any(Ruta.class));
  }

  @Test
  void procesarResultado_DonacionNoEnPlanner_SeQuedaEnAsignacionRealizada() {
    Camion camion = crearCamionDummy("ABC123");
    EntidadBeneficiaria entA = crearEntidad("Entidad A");
    EntidadBeneficiaria entB = crearEntidad("Entidad B");

    Donacion planificada = crearDonacion(EstadoDonacion.ASIGNACION_REALIZADA, entA);
    Donacion noPlanificada = crearDonacion(EstadoDonacion.ASIGNACION_REALIZADA, entB);
    List<Donacion> todas = List.of(planificada, noPlanificada);

    when(donacionesRepository.getDonacionesEnAsignacionRealizada()).thenReturn(todas);
    when(camionRepository.listarTodos()).thenReturn(List.of(camion));
    LoteId loteId = LoteId.generar();
    when(planificadorRutas.enviarLote(any())).thenReturn(loteId);

    servicio.planificar();

    // Only entA is in the planner response
    ResultadoPlanificacion resultado =
        new ResultadoPlanificacion(loteId, Map.of(camion, List.of(entA)));

    servicio.procesarResultado(resultado);

    // planificada transitioned
    assertEquals(EstadoDonacion.LISTA_PARA_ENTREGAR, planificada.getEstado());
    // noPlanificada stays
    assertEquals(EstadoDonacion.ASIGNACION_REALIZADA, noPlanificada.getEstado());
  }

  @Test
  void procesarResultado_ResultadoVacio_NoCreaRutas() {
    Camion camion = crearCamionDummy("ABC123");
    EntidadBeneficiaria entA = crearEntidad("Entidad A");
    Donacion donacion = crearDonacion(EstadoDonacion.ASIGNACION_REALIZADA, entA);

    when(donacionesRepository.getDonacionesEnAsignacionRealizada()).thenReturn(List.of(donacion));
    when(camionRepository.listarTodos()).thenReturn(List.of(camion));
    LoteId loteId = LoteId.generar();
    when(planificadorRutas.enviarLote(any())).thenReturn(loteId);

    servicio.planificar();

    // Empty planner response
    ResultadoPlanificacion resultado = new ResultadoPlanificacion(loteId, Map.of());

    servicio.procesarResultado(resultado);

    verify(rutaRepository, never()).agregar(any());
    assertEquals(EstadoDonacion.ASIGNACION_REALIZADA, donacion.getEstado());
  }
}
