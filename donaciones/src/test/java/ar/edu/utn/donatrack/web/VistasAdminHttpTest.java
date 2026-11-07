package ar.edu.utn.donatrack.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.donatrack.testsupport.ApiDePrueba;
import ar.edu.utn.donatrack.testsupport.EscenarioHttp;
import ar.edu.utn.donatrack.testsupport.EscenarioHttp.Archivo;
import ar.edu.utn.donatrack.testsupport.FlotaEnMemoria;
import io.javalin.Javalin;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Vistas de la administración: vencidas (#57), asignaciones (#58), camiones (#59) y CSV (#60). */
class VistasAdminHttpTest {
  private final FlotaEnMemoria flota = new FlotaEnMemoria();
  private Javalin app;
  private EscenarioHttp escenario;
  private String admin;

  @BeforeEach
  void start() throws Exception {
    app = ApiDePrueba.crear(Clock.systemUTC(), patente -> Optional.empty(), flota).start(0);
    escenario = new EscenarioHttp("http://localhost:" + app.port());
    admin = escenario.admin();
  }

  @AfterEach
  void stop() {
    escenario.limpiar();
    app.stop();
  }

  @Test
  void lasVencidasAparecenPrimeroYSeMarcanDeAUnaOTodas() throws Exception {
    UUID donante = escenario.crearDonante("Sofía", "30111222", "sofia@example.org");
    UUID vieja = escenario.crearDonacion(donante, "Leche", "2020-01-01");
    UUID otraVieja = escenario.crearDonacion(donante, "Yogur", "2020-02-01");
    UUID fresca = escenario.crearDonacion(donante, "Arroz", "2099-01-01");

    String pagina = escenario.get("/admin/vencidas", admin).body();
    assertTrue(pagina.indexOf("Leche") < pagina.indexOf("Arroz"), "las vencidas van primero");
    assertTrue(pagina.contains("Marcar las 2 vencidas"));

    assertEquals(
        302, escenario.formulario("/admin/vencidas/" + vieja, Map.of(), admin).statusCode());
    assertEquals("VENCIDA", escenario.estado(vieja));

    assertEquals(302, escenario.formulario("/admin/vencidas", Map.of(), admin).statusCode());
    assertEquals("VENCIDA", escenario.estado(otraVieja));
    assertEquals("EN_DEPOSITO", escenario.estado(fresca), "las que no vencieron no se tocan");
  }

  @Test
  void laAdministracionEligeLaEntidadEntreLasPropuestas() throws Exception {
    UUID donante = escenario.crearDonante("Sofía", "30111222", "sofia@example.org");
    long comedor = escenario.crearEntidad("Comedor Los Pibes");
    escenario.crearNecesidad(comedor, "Arroz");
    long ajena = escenario.crearEntidad("Sin necesidades");
    UUID donacion = escenario.crearDonacion(donante, "Arroz");

    assertTrue(escenario.get("/admin/asignaciones", admin).body().contains("1 sin propuesta"));
    assertEquals(302, escenario.formulario("/admin/asignaciones", Map.of(), admin).statusCode());
    String propuestas = escenario.get("/admin/asignaciones", admin).body();
    assertTrue(propuestas.contains("Comedor Los Pibes"));
    assertTrue(propuestas.contains("value=\"" + comedor + "\" checked"));

    HttpResponse<String> fuera =
        escenario.formulario(
            "/admin/asignaciones/" + donacion, Map.of("entidad", String.valueOf(ajena)), admin);
    assertEquals(400, fuera.statusCode());
    assertTrue(fuera.body().contains("no está entre las propuestas"));

    assertEquals(
        302,
        escenario
            .formulario(
                "/admin/asignaciones/" + donacion,
                Map.of("entidad", String.valueOf(comedor)),
                admin)
            .statusCode());
    assertEquals("ASIGNACION_REALIZADA", escenario.estado(donacion));
  }

  @Test
  void losCamionesSeAdministranEnLogisticaConLaSesionDeLaAdministracion() throws Exception {
    Map<String, String> camion =
        Map.of("patente", "ab123cd", "volumen", "30", "altura", "2.5", "capacidad", "3500");
    assertEquals(302, escenario.formulario("/admin/camiones", camion, admin).statusCode());
    assertEquals(3500, flota.flota().get("AB123CD").getCapacidadCargaKg());
    assertTrue(flota.sesiones().contains(admin.substring("access_token=".length())));
    assertTrue(escenario.get("/admin/camiones", admin).body().contains("AB123CD"));

    HttpResponse<String> repetido = escenario.formulario("/admin/camiones", camion, admin);
    assertEquals(400, repetido.statusCode());
    assertTrue(repetido.body().contains("Ya existe un camión"));

    HttpResponse<String> mala =
        escenario.formulario(
            "/admin/camiones",
            Map.of("patente", "X", "volumen", "1", "altura", "1", "capacidad", "1"),
            admin);
    assertEquals(400, mala.statusCode());

    Map<String, String> edicion =
        Map.of(
            "modo",
            "editar",
            "patente",
            "AB123CD",
            "volumen",
            "30",
            "altura",
            "2.5",
            "capacidad",
            "4000");
    assertEquals(302, escenario.formulario("/admin/camiones", edicion, admin).statusCode());
    assertEquals(4000, flota.flota().get("AB123CD").getCapacidadCargaKg());

    assertEquals(
        302,
        escenario.formulario("/admin/camiones/AB123CD/eliminar", Map.of(), admin).statusCode());
    assertTrue(flota.flota().isEmpty());
  }

  @Test
  void laImportacionCreaDonantesConCuentasQueCadaPersonaActiva() throws Exception {
    escenario.crearDonante("Ya Registrada", "20000001", "ya@example.org");
    String csv =
        "TipoPersona,TipoDoc,Documento,Nombre/Razón Social,Email,Teléfono\n"
            + "HUMANA,DNI,30111222,Ana Gómez,ana@example.org,1155550000\n"
            + "HUMANA,DNI,20000001,Ya Registrada,ya@example.org,\n"
            + "MARCIANA,DNI,1,Nadie,nadie@example.org,\n";

    HttpResponse<String> subida =
        escenario.subir(
            "/admin/importar",
            List.of(
                new Archivo(
                    "archivo", "donantes.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8))),
            admin);
    assertEquals(302, subida.statusCode(), subida.body());

    String pagina = esperarImportacion();
    assertTrue(pagina.contains("Terminada"), pagina);
    assertTrue(pagina.contains("Fila 4: tipo de persona o de documento inválido"));

    // Antes de activar no hay contraseña que funcione.
    assertEquals(
        200,
        escenario
            .formulario("/login", Map.of("email", "ana@example.org", "password", ""), "")
            .statusCode());
    HttpResponse<String> documentoAjeno =
        escenario.formulario(
            "/activar",
            Map.of(
                "email", "ana@example.org",
                "documento", "99999999",
                "password", "nueva-clave",
                "confirmacion", "nueva-clave"),
            "");
    assertEquals(400, documentoAjeno.statusCode());

    HttpResponse<String> activacion =
        escenario.formulario(
            "/activar",
            Map.of(
                "email", "ana@example.org",
                "documento", "30111222",
                "password", "nueva-clave",
                "confirmacion", "nueva-clave"),
            "");
    assertEquals(302, activacion.statusCode());
    String cookie = escenario.login("ana@example.org", "nueva-clave");
    assertTrue(escenario.get("/donante/donaciones", cookie).body().contains("Mis donaciones"));

    HttpResponse<String> deNuevo =
        escenario.formulario(
            "/activar",
            Map.of(
                "email", "ana@example.org",
                "documento", "30111222",
                "password", "otra-clave1",
                "confirmacion", "otra-clave1"),
            "");
    assertEquals(400, deNuevo.statusCode(), "una cuenta activa no se puede volver a activar");
  }

  @Test
  void soloLaAdministracionImporta() throws Exception {
    String donante = escenario.login("donante@donatrack.local", "donante123");
    assertEquals(403, escenario.get("/admin/importar", donante).statusCode());
    assertFalse(escenario.get("/admin/importar", admin).body().contains("Importando"));
  }

  private String esperarImportacion() throws Exception {
    for (int intento = 0; intento < 50; intento++) {
      String pagina = escenario.get("/admin/importar", admin).body();
      if (!pagina.contains("Importando…")) {
        return pagina;
      }
      Thread.sleep(100);
    }
    throw new AssertionError("La importación no terminó");
  }
}
