package ar.edu.utn.donatrack.logistica.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.donatrack.logistica.Camion;
import ar.edu.utn.donatrack.logistica.CamionRepository;
import ar.edu.utn.donatrack.logistica.DonacionId;
import ar.edu.utn.donatrack.logistica.EntidadBeneficiariaId;
import ar.edu.utn.donatrack.logistica.RutaRepository;
import ar.edu.utn.donatrack.logistica.monitoreo.Coordenada;
import ar.edu.utn.donatrack.logistica.planificacion.DonacionPlanificable;
import ar.edu.utn.donatrack.logistica.planificacion.DonacionesRemotas;
import ar.edu.utn.donatrack.logistica.planificacion.LoteId;
import ar.edu.utn.donatrack.logistica.planificacion.PlanificadorRutas;
import ar.edu.utn.donatrack.logistica.planificacion.ServicioPlanificacionRutas;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class PlanificacionControllerTest implements WithSimplePersistenceUnit {

  private static final String PATENTE = "AB123CD";
  private static final long ENTIDAD_ID = 45L;
  private static final String DIRECCION = "Av. Mitre 750";

  private RutaRepository rutaRepo;
  private CamionRepository camionRepo;
  private DonacionesRemotas donacionesRemotas;
  private PlanificacionController controlador;
  private Javalin app;

  private LoteId loteId;
  private DonacionId donacionId;

  @BeforeEach
  public void setUp() {
    rutaRepo = new RutaRepository();
    camionRepo = new CamionRepository();
    donacionesRemotas = Mockito.mock(DonacionesRemotas.class);
    loteId = LoteId.generar();
    donacionId = new DonacionId(UUID.randomUUID());

    PlanificadorRutas planificadorRutas = lote -> loteId;
    ServicioPlanificacionRutas servicio =
        new ServicioPlanificacionRutas(donacionesRemotas, camionRepo, planificadorRutas, rutaRepo);
    controlador = new PlanificacionController(servicio, camionRepo);

    // El repositorio ya no abre ninguna transacción por su cuenta y acá no hay una previa, así
    // que el test es el dueño legítimo de esta unidad de trabajo. La confirmación además hace
    // visible el camión para el hilo que atiende el callback.
    Camion camionPrueba = new Camion(PATENTE, 20.0, 2.5, 10000.0);
    withTransaction(() -> camionRepo.agregar(camionPrueba));

    when(donacionesRemotas.listarEnAsignacionRealizada()).thenReturn(List.of(crearPlanificable()));
    servicio.planificar();

    app =
        Javalin.create().post("/logistica/planificacion/callback", controlador::procesarResultado);
  }

  @AfterEach
  void limpiarPersistencia() {
    withTransaction(
        () ->
            createQuery("select c from Camion c", Camion.class)
                .getResultList()
                .forEach(this::remove));
    entityManager().clear();
  }

  @Test
  public void testCallbackDeLotePendienteDevuelve200YCreaLaRuta() {
    JavalinTest.test(
        app,
        (server, client) -> {
          var response =
              client.post("/logistica/planificacion/callback", json(PATENTE, donacionId));

          assertEquals(200, response.code());
          assertEquals(1, rutaRepo.listarTodas().size(), "Debería haberse creado 1 ruta");
          verify(donacionesRemotas).notificarListaParaEntregar(List.of(donacionId));
        });
  }

  @Test
  public void testCallbackDeLoteNoPendienteDevuelve404() {
    JavalinTest.test(
        app,
        (server, client) -> {
          String json =
              """
              {
                "loteId": "%s",
                "rutas": {
                  "%s": [
                    {
                      "entidadBeneficiariaId": %d,
                      "direccion": "%s",
                      "donaciones": ["%s"]
                    }
                  ]
                }
              }
              """
                  .formatted(UUID.randomUUID(), PATENTE, ENTIDAD_ID, DIRECCION, donacionId.value());

          var response = client.post("/logistica/planificacion/callback", json);

          assertEquals(404, response.code());
          assertEquals(0, rutaRepo.listarTodas().size(), "No debería crearse ninguna ruta");
          verify(donacionesRemotas, never()).notificarListaParaEntregar(anyList());
        });
  }

  @Test
  public void testCallbackConCamionInexistenteDevuelve400() {
    JavalinTest.test(
        app,
        (server, client) -> {
          var response =
              client.post("/logistica/planificacion/callback", json("FALSO99", donacionId));

          assertEquals(400, response.code());
          assertEquals(0, rutaRepo.listarTodas().size(), "No debería crearse ninguna ruta");
          verify(donacionesRemotas, never()).notificarListaParaEntregar(anyList());
        });
  }

  @Test
  public void testCallbackConDonacionFueraDelLoteDevuelve400() {
    JavalinTest.test(
        app,
        (server, client) -> {
          DonacionId ajena = new DonacionId(UUID.randomUUID());
          var response = client.post("/logistica/planificacion/callback", json(PATENTE, ajena));

          assertEquals(400, response.code());
          assertEquals(0, rutaRepo.listarTodas().size(), "No debería crearse ninguna ruta");
          verify(donacionesRemotas, never()).notificarListaParaEntregar(anyList());
        });
  }

  @Test
  public void testCallbackConBodyNoJsonDevuelve400() {
    JavalinTest.test(
        app,
        (server, client) -> {
          var response = client.post("/logistica/planificacion/callback", "esto no es json");

          assertEquals(400, response.code());
          assertEquals(0, rutaRepo.listarTodas().size(), "No debería crearse ninguna ruta");
        });
  }

  private String json(String patente, DonacionId idDonacion) {
    return """
        {
          "loteId": "%s",
          "rutas": {
            "%s": [
              {
                "entidadBeneficiariaId": %d,
                "direccion": "%s",
                "donaciones": ["%s"]
              }
            ]
          }
        }
        """
        .formatted(loteId.value(), patente, ENTIDAD_ID, DIRECCION, idDonacion.value());
  }

  private DonacionPlanificable crearPlanificable() {
    return new DonacionPlanificable(
        donacionId,
        new EntidadBeneficiariaId(ENTIDAD_ID),
        DIRECCION,
        new Coordenada(-34.6, -58.4),
        1.0,
        1.0);
  }
}
