package ar.edu.utn.donatrack.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.donatrack.beneficiarios.Necesidad;
import ar.edu.utn.donatrack.testsupport.ApiDePrueba;
import ar.edu.utn.donatrack.testsupport.EscenarioHttp;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.Javalin;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Los roles que declara cada ruta, aplicados por {@link ControlDeAcceso}. */
class ControlDeAccesoHttpTest implements WithSimplePersistenceUnit {
  private Javalin app;
  private EscenarioHttp escenario;

  @BeforeEach
  void start() throws Exception {
    app = ApiDePrueba.crear().start(0);
    escenario = new EscenarioHttp("http://localhost:" + app.port());
  }

  @AfterEach
  void stop() {
    escenario.limpiar();
    app.stop();
  }

  @Test
  void laAdministracionNoEntraALasPantallasDeDonanteNiDeEntidad() throws Exception {
    String admin = escenario.admin();
    for (String ruta :
        new String[] {
          "/donante/donaciones",
          "/donante/seguimiento",
          "/entidad/necesidades",
          "/entidad/recepciones"
        }) {
      assertEquals(403, escenario.get(ruta, admin).statusCode(), ruta);
    }
    // Sus propias pantallas, sí.
    assertEquals(200, escenario.get("/admin/vencidas", admin).statusCode());
    assertEquals(200, escenario.get("/dashboard", admin).statusCode());
  }

  @Test
  void enLaApiLaAdministracionSigueOperandoEnNombreDeLasPersonas() throws Exception {
    UUID donante = escenario.crearDonante("Sofía", "30111222", "sofia@example.org");
    String admin = escenario.admin();
    assertEquals(
        200, escenario.json("GET", "/api/v1/donantes/" + donante, null, admin).statusCode());
    assertEquals(200, escenario.json("GET", "/api/v1/donaciones", null, admin).statusCode());
  }

  @Test
  void cadaRolEntraSoloASusPantallas() throws Exception {
    UUID sofia = escenario.crearDonante("Sofía", "30111222", "sofia@example.org");
    String donante = escenario.sesionDeDonante(sofia, "sofia@example.org");
    long comedor = escenario.crearEntidad("Comedor Norte");
    String entidad = escenario.sesionDeEntidad(comedor, "comedor@example.org");

    assertEquals(200, escenario.get("/donante/donaciones", donante).statusCode());
    assertEquals(403, escenario.get("/entidad/necesidades", donante).statusCode());
    assertEquals(403, escenario.get("/admin/vencidas", donante).statusCode());

    assertEquals(200, escenario.get("/entidad/necesidades", entidad).statusCode());
    assertEquals(403, escenario.get("/donante/donaciones", entidad).statusCode());
    assertEquals(403, escenario.get("/admin/vencidas", entidad).statusCode());
  }

  @Test
  void sinSesionLaWebRedirigeAlLoginYNoEjecutaLaAccion() throws Exception {
    long comedor = escenario.crearEntidad("Comedor Norte");
    HttpResponse<String> alta =
        escenario.formulario(
            "/entidad/necesidades",
            Map.of("descripcion", "Arroz", "cantidad", "10", "entidadId", String.valueOf(comedor)),
            "");

    assertEquals(302, alta.statusCode());
    assertEquals("/login", alta.headers().firstValue("Location").orElse(""));
    entityManager().clear();
    assertTrue(
        createQuery("select n from Necesidad n", Necesidad.class).getResultList().isEmpty(),
        "el handler no tiene que correr sin sesión");
  }

  @Test
  void sinSesionLaApiResponde401() throws Exception {
    assertEquals(401, escenario.json("GET", "/api/v1/donaciones", null, "").statusCode());
  }

  @Test
  void cerrarSesionInvalidaElTokenEnElMomento() throws Exception {
    String admin = escenario.admin();
    assertEquals(200, escenario.get("/dashboard", admin).statusCode());

    escenario.formulario("/logout", Map.of(), admin);

    // El token sigue bien firmado y sin vencer, pero su sesión ya no existe.
    HttpResponse<String> despues = escenario.get("/dashboard", admin);
    assertEquals(302, despues.statusCode());
    assertEquals("/login", despues.headers().firstValue("Location").orElse(""));
  }
}
