package ar.edu.utn.donatrack.logistica.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.donatrack.logistica.Camion;
import ar.edu.utn.donatrack.logistica.CamionRepository;
import ar.edu.utn.donatrack.logistica.Ruta;
import ar.edu.utn.donatrack.logistica.RutaRepository;
import ar.edu.utn.donatrack.logistica.monitoreo.Coordenada;
import ar.edu.utn.donatrack.logistica.monitoreo.LecturaPosicion;
import ar.edu.utn.donatrack.logistica.monitoreo.MonitorRutas;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PosicionControllerTest {

  private CamionRepository camionRepoMock;
  private RutaRepository rutaRepoMock;
  private MonitorRutas monitorMock;
  private PosicionController controlador;
  private Javalin app;

  @BeforeEach
  public void setUp() {
    // 2. Creación de Mocks usando el método mock() como indica la guía
    camionRepoMock = mock(CamionRepository.class);
    rutaRepoMock = mock(RutaRepository.class);
    monitorMock = mock(MonitorRutas.class);

    controlador = new PosicionController(monitorMock, camionRepoMock, rutaRepoMock);

    app = Javalin.create().get("/logistica/camiones/{patente}/posicion", controlador::leerPosicion);
  }

  @Test
  public void testLeerPosicionCumpleContratoPunto4_ConMockito() {

    // --- PREPARACIÓN DE DATOS (Falsos) ---
    Camion camionFalso = new Camion("AF123ZZ", 20.0, 2.5, 10000.0);
    Coordenada coordFalsa = new Coordenada(-34.665, -58.370);
    LecturaPosicion lecturaFalsa =
        new LecturaPosicion(
            camionFalso, coordFalsa, 45.5, LocalDateTime.parse("2026-09-09T18:15:00"));

    // --- STUBBING DE RETORNOS (La magia de Mockito) ---
    // Le indicamos qué debe devolver cuando reciba los mensajes, sin ejecutar lógica real
    when(camionRepoMock.buscarPorPatente("AF123ZZ")).thenReturn(Optional.of(camionFalso));
    when(monitorMock.ultimaPosicion(camionFalso)).thenReturn(Optional.of(lecturaFalsa));

    // Simulamos que el camión está en una ruta activa para cumplir el JSON de salida. La ruta
    // lleva un id explícito porque representa una ruta ya materializada, con id del proveedor.
    Ruta rutaFalsa = new Ruta(java.util.UUID.randomUUID(), camionFalso, java.util.List.of());
    when(rutaRepoMock.buscarPorCamion(camionFalso)).thenReturn(Optional.of(rutaFalsa));

    // --- EJECUCIÓN Y VERIFICACIÓN ---
    JavalinTest.test(
        app,
        (server, client) -> {
          var response = client.get("/logistica/camiones/AF123ZZ/posicion");

          assertEquals(200, response.code());
          String responseBody = response.body().string();

          // Verificamos el contrato
          assertTrue(responseBody.contains("\"idCamion\":\"AF123ZZ\""));
          assertTrue(responseBody.contains("\"lat\":-34.665"));
          assertTrue(responseBody.contains("\"lng\":-58.37"));
        });
  }
}
