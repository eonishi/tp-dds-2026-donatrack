package ar.edu.utn.donatrack.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.donatrack.testsupport.ApiDePrueba;
import ar.edu.utn.donatrack.testsupport.EscenarioHttp;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** La administración consulta las entidades beneficiarias cargadas en el sistema. */
class AdminEntidadesHttpTest {
  private Javalin app;
  private EscenarioHttp escenario;
  private String admin;

  @BeforeEach
  void start() throws Exception {
    app = ApiDePrueba.crear().start(0);
    escenario = new EscenarioHttp("http://localhost:" + app.port());
    admin = escenario.admin();
  }

  @AfterEach
  void stop() {
    escenario.limpiar();
    app.stop();
  }

  @Test
  void elListadoMuestraContactoYCuentaYBuscaPorRazonSocialOCorreo() throws Exception {
    long comedor = escenario.crearEntidad("Comedor Norte");
    escenario.crearEntidad("Hogar del Sur");
    escenario.sesionDeEntidad(comedor, "comedor@example.org");

    String todas = escenario.get("/admin/entidades", admin).body();
    assertTrue(todas.contains("Comedor Norte"));
    assertTrue(todas.contains("Hogar del Sur"));
    assertTrue(todas.contains("entidad@example.org"));
    assertTrue(todas.contains("Con cuenta"));
    assertTrue(todas.contains("Sin cuenta"));

    String porNombre = escenario.get("/admin/entidades?q=hogar", admin).body();
    assertTrue(porNombre.contains("Hogar del Sur"));
    assertFalse(porNombre.contains("Comedor Norte"));

    // Por correo, sin importar mayúsculas: las dos tienen entidad@example.org.
    String porCorreo = escenario.get("/admin/entidades?q=ENTIDAD%40example", admin).body();
    assertTrue(porCorreo.contains("2 entidades para «ENTIDAD@example»"), porCorreo);
    assertFalse(porCorreo.contains("Entidad beneficiaria de ejemplo"));
  }

  @Test
  void elListadoSePagina() throws Exception {
    for (int i = 0; i < 27; i++) {
      escenario.crearEntidad(String.format("Entidad %02d", i));
    }
    String primera = escenario.get("/admin/entidades", admin).body();
    assertTrue(primera.contains("página 1 de 2"));
    assertTrue(primera.contains("Entidad 00"));
    assertFalse(primera.contains("Entidad 26"));

    String segunda = escenario.get("/admin/entidades?pagina=2", admin).body();
    assertTrue(segunda.contains("Entidad 26"));
    assertTrue(segunda.contains("Anterior"));
    assertEquals(200, escenario.get("/admin/entidades?pagina=abc", admin).statusCode());
  }
}
