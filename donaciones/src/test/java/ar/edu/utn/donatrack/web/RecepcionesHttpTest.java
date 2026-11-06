package ar.edu.utn.donatrack.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.donatrack.testsupport.ApiDePrueba;
import ar.edu.utn.donatrack.testsupport.EscenarioHttp;
import ar.edu.utn.donatrack.testsupport.EscenarioHttp.Archivo;
import io.javalin.Javalin;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** La entidad beneficiaria confirma con fotos la recepción de lo que le asignaron (#53). */
class RecepcionesHttpTest {
  private Javalin app;
  private EscenarioHttp escenario;
  private long entidad;
  private UUID donacion;
  private String cookieEntidad;

  @BeforeEach
  void start() throws Exception {
    app = ApiDePrueba.crear().start(0);
    escenario = new EscenarioHttp("http://localhost:" + app.port());
    UUID donante = escenario.crearDonante("Sofía", "30111222", "sofia@example.org");
    entidad = escenario.crearEntidad("Comedor Los Pibes");
    escenario.crearNecesidad(entidad, "Arroz");
    donacion = escenario.crearDonacion(donante, "Arroz");
    escenario.llevarATraslado(donacion, entidad);
    cookieEntidad = escenario.sesionDeEntidad(entidad, "comedor@example.org");
  }

  @AfterEach
  void stop() {
    escenario.limpiar();
    app.stop();
  }

  @Test
  void laEntidadVeLoQueEstaEnCaminoConElCamion() throws Exception {
    HttpResponse<String> pagina = escenario.get("/entidad/recepciones", cookieEntidad);

    assertEquals(200, pagina.statusCode());
    assertTrue(pagina.body().contains("10 kilogramos de Arroz"));
    assertTrue(pagina.body().contains(EscenarioHttp.PATENTE));
    assertTrue(pagina.body().contains("/entidad/recepciones/" + donacion + "/confirmar"));
  }

  @Test
  void confirmaConFotosYLaEntregaQuedaPublicadaConEllas() throws Exception {
    HttpResponse<String> respuesta =
        escenario.subir(
            "/entidad/recepciones/" + donacion + "/confirmar",
            List.of(new Archivo("fotos", "recibido.png", "image/png", EscenarioHttp.png())),
            cookieEntidad);

    assertEquals(302, respuesta.statusCode(), respuesta.body());
    assertEquals(
        "/entidad/recepciones?ok=confirmada", respuesta.headers().firstValue("Location").get());
    assertEquals("ENTREGADA", escenario.estado(donacion));

    String publica = escenario.get("/entregas", "").body();
    Matcher foto = Pattern.compile("/fotos/" + donacion + "-[a-z0-9-]+\\.png").matcher(publica);
    assertTrue(foto.find(), "la foto debe aparecer en la página pública de entregas");
    HttpResponse<byte[]> imagen = escenario.getBytes(foto.group());
    assertEquals(200, imagen.statusCode());
    assertEquals("image/png", imagen.headers().firstValue("Content-Type").get());
  }

  @Test
  void sinFotosOConUnArchivoQueNoEsImagenNoConfirma() throws Exception {
    HttpResponse<String> sinFotos =
        escenario.subir(
            "/entidad/recepciones/" + donacion + "/confirmar", List.of(), cookieEntidad);
    assertEquals(400, sinFotos.statusCode());
    assertTrue(sinFotos.body().contains("Subí al menos una foto"));

    HttpResponse<String> falsa =
        escenario.subir(
            "/entidad/recepciones/" + donacion + "/confirmar",
            List.of(
                new Archivo(
                    "fotos",
                    "foto.png",
                    "image/png",
                    "<script>no soy una imagen</script>".getBytes(StandardCharsets.UTF_8))),
            cookieEntidad);
    assertEquals(400, falsa.statusCode());
    assertTrue(falsa.body().contains("no es JPG, PNG ni WebP"));
    assertTrue(falsa.body().contains("<nav"), "el error se muestra en la misma vista");

    assertEquals("EN_TRASLADO", escenario.estado(donacion));
  }

  @Test
  void reportaUnProblemaConElMotivo() throws Exception {
    HttpResponse<String> respuesta =
        escenario.formulario(
            "/entidad/recepciones/" + donacion + "/reportar",
            Map.of("motivo", "Llegó con los paquetes rotos"),
            cookieEntidad);

    assertEquals(302, respuesta.statusCode(), respuesta.body());
    assertEquals("ENTREGA_FALLIDA", escenario.estado(donacion));
  }

  @Test
  void otraEntidadNoPuedeConfirmarUnaDonacionAjena() throws Exception {
    long otra = escenario.crearEntidad("Otra entidad");
    String cookieOtra = escenario.sesionDeEntidad(otra, "otra@example.org");

    HttpResponse<String> respuesta =
        escenario.subir(
            "/entidad/recepciones/" + donacion + "/confirmar",
            List.of(new Archivo("fotos", "x.png", "image/png", EscenarioHttp.png())),
            cookieOtra);

    assertEquals(404, respuesta.statusCode());
    assertFalse(escenario.get("/entidad/recepciones", cookieOtra).body().contains("Arroz"));
    assertEquals("EN_TRASLADO", escenario.estado(donacion));
  }

  @Test
  void soloEntraUnaEntidadConSesion() throws Exception {
    HttpResponse<String> sinSesion = escenario.get("/entidad/recepciones", "");
    assertEquals(302, sinSesion.statusCode());
    assertEquals("/login", sinSesion.headers().firstValue("Location").get());

    String donante = escenario.login("donante@donatrack.local", "donante123");
    assertEquals(403, escenario.get("/entidad/recepciones", donante).statusCode());
  }

  @Test
  void lasFotosLocalesNoPermitenSalirDelDirectorio() throws Exception {
    // Jetty ya rechaza las barras codificadas (400); lo que llegue al controller sin un nombre
    // generado por el sistema da 404.
    int barras = escenario.getBytes("/fotos/..%2F..%2Fetc%2Fpasswd").statusCode();
    assertTrue(barras == 400 || barras == 404, "debe rechazarse, fue " + barras);
    assertEquals(404, escenario.getBytes("/fotos/..passwd").statusCode());
    assertEquals(404, escenario.getBytes("/fotos/no-existe.png").statusCode());
  }
}
