package ar.edu.utn.donatrack.logistica.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ar.edu.utn.donatrack.logistica.Camion;
import ar.edu.utn.donatrack.logistica.CamionRepository;
import ar.edu.utn.donatrack.logistica.Destino;
import ar.edu.utn.donatrack.logistica.DonacionId;
import ar.edu.utn.donatrack.logistica.EntidadBeneficiariaId;
import ar.edu.utn.donatrack.logistica.Ruta;
import ar.edu.utn.donatrack.logistica.RutaRepository;
import ar.edu.utn.donatrack.logistica.planificacion.DonacionesRemotas;
import ar.edu.utn.donatrack.persistencia.JpaRepository;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;

public class RutaControllerTest implements JpaRepository {

  private RutaRepository rutaRepo;
  private CamionRepository camionRepo;
  private RutaController controlador;
  private Javalin app;
  private DonacionesRemotas donacionesRemotas;

  @AfterEach
  void limpiarPersistencia() {
    // Las solicitudes HTTP confirman sus propias transacciones, a diferencia de las pruebas
    // transaccionales que revierten: se borran los camiones confirmados en el setUp para que no
    // contaminen los conteos de las demás pruebas. Después se limpia el contexto para que el
    // próximo test no reutilice una instancia administrada con la misma patente.
    inTransaction(
        () ->
            createQuery("select c from Camion c", Camion.class)
                .getResultList()
                .forEach(this::remove));
    entityManager().clear();
  }

  @BeforeEach
  public void setUp() {
    rutaRepo = new RutaRepository();
    camionRepo = new CamionRepository();
    donacionesRemotas = mock(DonacionesRemotas.class);
    controlador = new RutaController(rutaRepo, camionRepo, donacionesRemotas);

    // Pre-cargamos un camión válido porque la ruta necesita uno para crearse
    Camion camionPrueba = new Camion("AB123CD", 20.0, 2.5, 10000.0);
    camionRepo.agregar(camionPrueba);

    app =
        Javalin.create()
            .post("/logistica/rutas", controlador::crear)
            .get("/logistica/rutas", controlador::listar);
  }

  @Test
  public void testCrearRutaConCamionExistenteDevuelve201() {
    JavalinTest.test(
        app,
        (server, client) -> {

          // Enviamos un JSON con la patente del camión que creamos en el setUp y sin destinos
          // (lista vacía)
          String jsonRequest =
              """
                    {
                      "patenteCamion": "AB123CD",
                      "destinos": []
                    }
                    """;

          var response = client.post("/logistica/rutas", jsonRequest);

          // Verificamos que el servidor haya respondido 201 Created
          assertEquals(201, response.code());

          // Verificamos que la ruta efectivamente se haya guardado en el repositorio
          assertEquals(1, rutaRepo.listarTodas().size(), "El repositorio debería tener 1 ruta");

          // EXTRAEMOS EL BODY UNA SOLA VEZ
          String responseBody = response.body().string();

          // Verificamos que el JSON de respuesta contenga los datos correctos
          assertTrue(responseBody.contains("AB123CD"));
          assertTrue(responseBody.contains("\"enCurso\":false"));
        });
  }

  @Test
  public void testCrearRutaConCamionInexistenteDevuelve400() {
    JavalinTest.test(
        app,
        (server, client) -> {

          // Enviamos un JSON con una patente que no existe en el repositorio
          String jsonRequest =
              """
                    {
                      "patenteCamion": "FALSO99",
                      "destinos": []
                    }
                    """;

          var response = client.post("/logistica/rutas", jsonRequest);

          // Verificamos que tu regla de negocio ataje el error y devuelva Bad Request
          assertEquals(400, response.code());
          assertEquals(
              0, rutaRepo.listarTodas().size(), "No se debería haber guardado ninguna ruta");
        });
  }

  @Test
  void iniciarRutaNotificaLaPatenteRealUnaSolaVez() {
    DonacionId donacionId = new DonacionId(UUID.randomUUID());
    Ruta ruta =
        new Ruta(
            camionRepo.buscarPorPatente("AB123CD").orElseThrow(),
            List.of(
                new Destino(
                    new EntidadBeneficiariaId(1L), "Calle 123", List.of(donacionId))));
    rutaRepo.agregar(ruta);
    Javalin appPatch =
        Javalin.create().patch("/logistica/rutas/{id}", controlador::actualizar);

    JavalinTest.test(
        appPatch,
        (server, client) -> {
          var response =
              client.patch(
                  "/logistica/rutas/" + ruta.getId(), "{\"estado\":\"EN_CURSO\"}");

          assertEquals(200, response.code());
          verify(donacionesRemotas)
              .notificarInicioTraslado(List.of(donacionId), "AB123CD");
        });
  }
}
