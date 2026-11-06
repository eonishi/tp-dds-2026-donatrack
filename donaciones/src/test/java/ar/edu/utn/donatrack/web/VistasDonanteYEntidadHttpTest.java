package ar.edu.utn.donatrack.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.donatrack.seguimiento.PosicionCamion;
import ar.edu.utn.donatrack.testsupport.ApiDePrueba;
import ar.edu.utn.donatrack.testsupport.EscenarioHttp;
import ar.edu.utn.donatrack.testsupport.FlotaEnMemoria;
import ar.edu.utn.donatrack.ubicacion.UbicadorEntidades;
import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.Javalin;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Vistas de la persona donante (#46, #49, #50) y de la entidad beneficiaria (#51, #54, #55), más el
 * mapa público de entregas (#43).
 */
class VistasDonanteYEntidadHttpTest {
  private Javalin app;
  private EscenarioHttp escenario;
  private UUID sofia;
  private long comedor;
  private String cookieSofia;
  private String cookieComedor;

  @BeforeEach
  void start() throws Exception {
    app =
        ApiDePrueba.crear(
                Clock.systemUTC(),
                patente ->
                    EscenarioHttp.PATENTE.equals(patente)
                        ? Optional.of(
                            new PosicionCamion(patente, -34.62, -58.44, "2026-11-06T10:15:00"))
                        : Optional.empty(),
                new FlotaEnMemoria())
            .start(0);
    escenario = new EscenarioHttp("http://localhost:" + app.port());
    sofia = escenario.crearDonante("Sofía", "30111222", "sofia@example.org");
    comedor = escenario.crearEntidad("Comedor Los Pibes");
    escenario.crearNecesidad(comedor, "Arroz");
    escenario.crearNecesidad(comedor, "Fideos");
    cookieSofia = escenario.sesionDeDonante(sofia, "sofia@example.org");
    cookieComedor = escenario.sesionDeEntidad(comedor, "comedor@example.org");
  }

  @AfterEach
  void stop() {
    escenario.limpiar();
    app.stop();
  }

  // --- Persona donante ---

  @Test
  void laDonanteFiltraSusDonacionesPorEstadoYSubcategoria() throws Exception {
    UUID arroz = escenario.crearDonacion(sofia, "Arroz");
    escenario.crearDonacion(sofia, "Fideos");
    escenario.llevarATraslado(arroz, comedor);

    String todas = escenario.get("/donante/donaciones", cookieSofia).body();
    assertTrue(
        todas.contains("10 kilogramos de Arroz") && todas.contains("10 kilogramos de Fideos"));

    String enCamino = escenario.get("/donante/donaciones?estado=EN_TRASLADO", cookieSofia).body();
    assertTrue(enCamino.contains("Arroz"));
    assertFalse(enCamino.contains("10 kilogramos de Fideos"));
    assertTrue(enCamino.contains("Ver el camión en el mapa"));

    String fideos = escenario.get("/donante/donaciones?subcategoria=Fideos", cookieSofia).body();
    assertTrue(fideos.contains("10 kilogramos de Fideos"));
    assertFalse(fideos.contains("10 kilogramos de Arroz"));

    String vestimenta =
        escenario.get("/donante/donaciones?categoria=VESTIMENTA", cookieSofia).body();
    assertTrue(vestimenta.contains("Ninguna donación coincide"));
  }

  @Test
  void laDonanteNoVeDonacionesAjenas() throws Exception {
    UUID otra = escenario.crearDonante("Lucía", "30999888", "lucia@example.org");
    escenario.crearDonacion(otra, "Fideos");

    String html = escenario.get("/donante/donaciones", cookieSofia).body();

    assertFalse(html.contains("Fideos</li>"));
    assertTrue(html.contains("Todavía no registramos donaciones tuyas"));
  }

  @Test
  void laDonanteSigueElCamionConSuUbicacionYElDestino() throws Exception {
    UUID arroz = escenario.crearDonacion(sofia, "Arroz");
    escenario.llevarATraslado(arroz, comedor);
    new UbicadorEntidades(direccion -> Optional.of(ApiDePrueba.UBICACION), 0).ubicarPendientes();

    HttpResponse<String> pagina = escenario.get("/donante/seguimiento", cookieSofia);
    assertEquals(200, pagina.statusCode());
    assertTrue(pagina.body().contains("data-mapa=\"seguimiento\""));
    assertTrue(pagina.body().contains("Camión " + EscenarioHttp.PATENTE));

    JsonNode entregas =
        escenario.leer(escenario.get("/donante/seguimiento/posiciones", cookieSofia));
    assertEquals(1, entregas.size());
    JsonNode entrega = entregas.get(0);
    assertEquals(EscenarioHttp.PATENTE, entrega.get("camion").asText());
    assertEquals(-34.62, entrega.get("latitud").asDouble());
    assertEquals("06/11/2026 10:15", entrega.get("actualizadaEn").asText());
    assertEquals(ApiDePrueba.UBICACION.latitud(), entrega.get("destinoLatitud").asDouble());
    assertEquals("Comedor Los Pibes", entrega.get("entidad").asText());
  }

  @Test
  void laDonanteVeSusAvisosYSeMarcanLeidos() throws Exception {
    UUID arroz = escenario.crearDonacion(sofia, "Arroz");
    escenario.asignar(arroz, comedor);

    String primera = escenario.get("/donante/notificaciones", cookieSofia).body();
    assertTrue(primera.contains("Tu donación ha sido asignada a Comedor Los Pibes."));
    assertTrue(primera.contains(">Nuevo<"));

    String segunda = escenario.get("/donante/notificaciones", cookieSofia).body();
    assertTrue(segunda.contains("Tu donación ha sido asignada"));
    assertFalse(segunda.contains(">Nuevo<"), "al volver ya están leídos");
  }

  // --- Entidad beneficiaria ---

  @Test
  void laEntidadCargaYEliminaSusNecesidades() throws Exception {
    HttpResponse<String> alta =
        escenario.formulario(
            "/entidad/necesidades",
            Map.of(
                "tipo", "RECURRENTE",
                "periodo", "MENSUAL",
                "categoria", "VESTIMENTA",
                "subcategoria", "Camperas",
                "cantidad", "20",
                "unidad", "UNIDADES",
                "descripcion", "Para el invierno"),
            cookieComedor);
    assertEquals(302, alta.statusCode(), alta.body());

    String lista = escenario.get("/entidad/necesidades", cookieComedor).body();
    assertTrue(lista.contains("Camperas"));
    assertTrue(lista.contains("Todos los meses"));
    assertTrue(lista.contains("0 de 20 unidades"));

    String id =
        lista.split("/entidad/necesidades/")[1].substring(0, 36); // primer formulario de baja
    assertEquals(
        302,
        escenario
            .formulario("/entidad/necesidades/" + id + "/eliminar", Map.of(), cookieComedor)
            .statusCode());
  }

  @Test
  void unaNecesidadIncompletaMuestraElErrorSinPerderLoCargado() throws Exception {
    HttpResponse<String> respuesta =
        escenario.formulario(
            "/entidad/necesidades",
            Map.of(
                "tipo", "EXTRAORDINARIA",
                "categoria", "ALIMENTO",
                "subcategoria", "Leche",
                "cantidad", "cero",
                "unidad", "LITROS",
                "descripcion", "Para la merienda"),
            cookieComedor);

    assertEquals(400, respuesta.statusCode());
    assertTrue(respuesta.body().contains("La cantidad tiene que ser un número."));
    assertTrue(respuesta.body().contains("value=\"Leche\""));
  }

  @Test
  void otraEntidadNoPuedeEliminarNecesidadesAjenas() throws Exception {
    String lista = escenario.get("/entidad/necesidades", cookieComedor).body();
    String id = lista.split("/entidad/necesidades/")[1].substring(0, 36);
    long otra = escenario.crearEntidad("Otra entidad");
    String cookieOtra = escenario.sesionDeEntidad(otra, "otra@example.org");

    assertEquals(
        404,
        escenario
            .formulario("/entidad/necesidades/" + id + "/eliminar", Map.of(), cookieOtra)
            .statusCode());
  }

  @Test
  void laEntidadVeSusAvisosYSigueLoQueVaARecibir() throws Exception {
    UUID arroz = escenario.crearDonacion(sofia, "Arroz");
    escenario.llevarATraslado(arroz, comedor);

    assertTrue(
        escenario
            .get("/entidad/notificaciones", cookieComedor)
            .body()
            .contains("Se ha asignado una donación a tu entidad."));
    JsonNode entregas =
        escenario.leer(escenario.get("/entidad/seguimiento/posiciones", cookieComedor));
    assertEquals(1, entregas.size());
    assertEquals(EscenarioHttp.PATENTE, entregas.get(0).get("camion").asText());
  }

  @Test
  void cadaRolSoloEntraASusVistas() throws Exception {
    assertEquals(403, escenario.get("/entidad/necesidades", cookieSofia).statusCode());
    assertEquals(403, escenario.get("/donante/donaciones", cookieComedor).statusCode());
    assertEquals(403, escenario.get("/admin/vencidas", cookieSofia).statusCode());
    assertEquals(302, escenario.get("/donante/seguimiento", "").statusCode());
  }

  // --- Mapa público (#43) ---

  @Test
  void elMapaPublicoMuestraLasEntregasPorEntidadSinDatosDeLaDonante() throws Exception {
    UUID arroz = escenario.crearDonacion(sofia, "Arroz");
    escenario.llevarATraslado(arroz, comedor);
    assertEquals(
        200,
        escenario
            .json(
                "PATCH",
                "/api/v1/donaciones/" + arroz + "/estado",
                "{\"estado\":\"ENTREGADA\"}",
                escenario.admin())
            .statusCode());

    assertEquals(0, escenario.leer(escenario.get("/entregas/mapa", "")).size(), "sin ubicar aún");
    new UbicadorEntidades(direccion -> Optional.of(ApiDePrueba.UBICACION), 0).ubicarPendientes();

    HttpResponse<String> mapa = escenario.get("/entregas/mapa", "");
    JsonNode marcadores = escenario.leer(mapa);
    assertEquals(1, marcadores.size());
    assertEquals("Comedor Los Pibes", marcadores.get(0).get("entidad").asText());
    assertEquals(ApiDePrueba.UBICACION.longitud(), marcadores.get(0).get("longitud").asDouble());
    assertEquals(
        "10 kilogramos de Arroz",
        marcadores.get(0).get("entregas").get(0).get("bienes").get(0).asText());
    assertFalse(mapa.body().contains("sofia") || mapa.body().contains("30111222"));
    assertTrue(escenario.get("/entregas", "").body().contains("data-mapa=\"entregas\""));
  }
}
