package ar.edu.utn.donatrack.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.donatrack.testsupport.ApiDePrueba;
import ar.edu.utn.donatrack.testsupport.EscenarioHttp;
import io.javalin.Javalin;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** El login es uno solo y cada rol entra a su panel con sus secciones (#44). */
class PanelPorRolHttpTest {
  private Javalin app;
  private EscenarioHttp escenario;

  @BeforeEach
  void start() throws Exception {
    app = ApiDePrueba.crear().start(0);
    escenario = new EscenarioHttp("http://localhost:" + app.port());
  }

  @AfterEach
  void stop() {
    app.stop();
  }

  @Test
  void losTresRolesIngresanPorElMismoLoginYVanASuPanel() throws Exception {
    for (String[] rol :
        new String[][] {
          {"admin@donatrack.local", "admin123", "Administración"},
          {"donante@donatrack.local", "donante123", "Persona donante"},
          {"beneficiario@donatrack.local", "beneficiario123", "Entidad beneficiaria"}
        }) {
      HttpResponse<String> login =
          escenario.formulario("/login", Map.of("email", rol[0], "password", rol[1]), "");
      assertEquals(302, login.statusCode(), rol[0]);
      assertEquals("/dashboard", login.headers().firstValue("Location").get(), rol[0]);

      String panel = escenario.get("/dashboard", escenario.login(rol[0], rol[1])).body();
      assertTrue(panel.contains(rol[2]), rol[0]);
    }
  }

  @Test
  void soloLaEntidadVeLaSeccionParaRecibirDonaciones() throws Exception {
    String entidad = escenario.login("beneficiario@donatrack.local", "beneficiario123");
    String donante = escenario.login("donante@donatrack.local", "donante123");

    assertTrue(escenario.get("/dashboard", entidad).body().contains("/entidad/recepciones"));
    assertFalse(escenario.get("/dashboard", donante).body().contains("/entidad/recepciones"));
  }

  @Test
  void unLoginFallidoConservaElCorreoPeroNoLaContrasena() throws Exception {
    HttpResponse<String> respuesta =
        escenario.formulario(
            "/login", Map.of("email", "alguien@example.org", "password", "secreta-123"), "");

    assertTrue(respuesta.body().contains("Correo o contraseña incorrectos."));
    assertTrue(respuesta.body().contains("value=\"alguien@example.org\""));
    assertFalse(respuesta.body().contains("secreta-123"));
  }

  @Test
  void losFormulariosNoPidenIdentificadoresATipear() throws Exception {
    String admin = escenario.login("admin@donatrack.local", "admin123");

    for (String seccion : new String[] {"/donaciones", "/necesidades", "/matchmaking"}) {
      String html = escenario.get(seccion, admin).body();
      assertFalse(html.contains("UUID"), seccion);
      assertFalse(html.contains("ID de"), seccion);
    }
    assertTrue(escenario.get("/donaciones", admin).body().contains("id=\"select-donante\""));
    assertTrue(escenario.get("/matchmaking", admin).body().contains("id=\"select-donacion\""));
  }

  @Test
  void laEntidadCargaNecesidadesDeLaSuyaSinElegirla() throws Exception {
    long entidad = escenario.crearEntidad("Comedor Propio");
    String cookie = escenario.sesionDeEntidad(entidad, "propio@example.org");

    String html = escenario.get("/necesidades", cookie).body();

    assertTrue(
        html.contains(
            "type=\"hidden\" id=\"select-entidad\" name=\"entidadId\" value=\"" + entidad + "\""));
    assertTrue(html.contains("Necesidades de tu entidad"));
    escenario.limpiar();
  }
}
