package ar.edu.utn.donatrack.logistica.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.donatrack.logistica.Camion;
import ar.edu.utn.donatrack.logistica.CamionRepository;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CamionControllerTest implements WithSimplePersistenceUnit {

  private CamionRepository repositorio;
  private CamionController controlador;
  private Javalin app;

  @AfterEach
  void limpiarPersistencia() {
    // Las solicitudes HTTP confirman sus propias transacciones, a diferencia de las pruebas
    // transaccionales que revierten: se borran los camiones confirmados para que no contaminen
    // los conteos de las demás pruebas. Después se limpia el contexto para que el próximo test no
    // reutilice una instancia administrada con la misma patente.
    withTransaction(
        () ->
            createQuery("select c from Camion c", Camion.class)
                .getResultList()
                .forEach(this::remove));
    entityManager().clear();
  }

  @BeforeEach
  public void setUp() {
    // Antes de cada test, arrancamos con un repositorio limpio
    repositorio = new CamionRepository();
    controlador = new CamionController(repositorio);

    // Configuramos una app Javalin "falsa" solo con los endpoints que queremos testear
    app =
        Javalin.create()
            .get("/logistica/camiones", controlador::listar)
            .post("/logistica/camiones", controlador::crear);
    // El contexto de persistencia es por hilo y la biblioteca no lo limpia: sin este reseteo, un
    // hilo reutilizado por Javalin serviría entidades cacheadas de una solicitud anterior.
    app.before(ctx -> entityManager().clear());
  }

  @Test
  public void testCrearCamionDevuelve201YLoGuardaEnElRepositorio() {
    JavalinTest.test(
        app,
        (server, client) -> {
          String jsonRequest =
              """
                    {
                      "patente": "AF123ZZ",
                      "volumenM3": 20.5,
                      "alturaM": 2.5,
                      "capacidadCargaKg": 15000.0
                    }
                    """;

          // Act: Simulamos un POST
          var response = client.post("/logistica/camiones", jsonRequest);

          // Assert 1: Código HTTP
          assertEquals(201, response.code(), "Debería devolver 201 Created");

          // Assert 2: Respuesta JSON
          assertTrue(response.body().string().contains("AF123ZZ"));

          // Assert 3: Verificamos el tamaño de la lista
          assertEquals(
              1, repositorio.listarTodos().size(), "El repositorio debería tener 1 camión");

          // Assert 4: Evaluamos los valores exactos del objeto guardado
          Camion camionGuardado = repositorio.listarTodos().get(0);

          assertEquals("AF123ZZ", camionGuardado.getPatente());
          assertEquals(20.5, camionGuardado.getVolumenM3());
          assertEquals(2.5, camionGuardado.getAlturaM());
          assertEquals(15000.0, camionGuardado.getCapacidadCargaKg());
        });
  }

  @Test
  public void testListarCamionesDevuelve200YListaVaciaAlIniciar() {
    JavalinTest.test(
        app,
        (server, client) -> {

          // Act: Hacemos un GET
          var response = client.get("/logistica/camiones");

          // Assert: Verificamos que responda OK y una lista vacía
          assertEquals(200, response.code());
          assertEquals("[]", response.body().string());
        });
  }
}
