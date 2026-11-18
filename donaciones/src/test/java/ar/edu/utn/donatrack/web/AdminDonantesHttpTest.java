package ar.edu.utn.donatrack.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.PersonajeJuridica;
import ar.edu.utn.donatrack.security.Usuario;
import ar.edu.utn.donatrack.testsupport.ApiDePrueba;
import ar.edu.utn.donatrack.testsupport.EscenarioHttp;
import ar.edu.utn.donatrack.testsupport.FlotaEnMemoria;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.Javalin;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** La administración lista, busca y registra personas donantes (entrega 4, punto 15). */
class AdminDonantesHttpTest implements WithSimplePersistenceUnit {
  private Javalin app;
  private EscenarioHttp escenario;
  private String admin;

  @BeforeEach
  void start() throws Exception {
    app =
        ApiDePrueba.crear(Clock.systemUTC(), patente -> Optional.empty(), new FlotaEnMemoria())
            .start(0);
    escenario = new EscenarioHttp("http://localhost:" + app.port());
    admin = escenario.admin();
  }

  @AfterEach
  void stop() {
    escenario.limpiar();
    app.stop();
  }

  @Test
  void elListadoBuscaPorNombreDocumentoOCorreoYMuestraElEstadoDeLaCuenta() throws Exception {
    UUID sofia = escenario.crearDonante("Sofía", "30111222", "sofia@example.org");
    escenario.crearDonante("Martín", "28999000", "martin@example.org");
    escenario.sesionDeDonante(sofia, "sofia@example.org");

    String todos = escenario.get("/admin/donantes", admin).body();
    assertTrue(todos.contains("Sofía Pérez"));
    assertTrue(todos.contains("Martín Pérez"));
    assertTrue(todos.contains("Activa"));
    assertTrue(todos.contains("Sin cuenta"));

    String porNombre = escenario.get("/admin/donantes?q=sof", admin).body();
    assertTrue(porNombre.contains("Sofía Pérez"));
    assertFalse(porNombre.contains("Martín Pérez"));

    assertTrue(escenario.get("/admin/donantes?q=28999", admin).body().contains("Martín Pérez"));
    assertTrue(
        escenario.get("/admin/donantes?q=MARTIN%40example", admin).body().contains("Martín"));
  }

  @Test
  void elListadoSePagina() throws Exception {
    for (int i = 0; i < 27; i++) {
      escenario.crearDonante("Persona" + i, String.valueOf(40000000 + i), "p" + i + "@example.org");
    }
    String primera = escenario.get("/admin/donantes", admin).body();
    assertTrue(primera.contains("página 1 de 2"));
    assertTrue(primera.contains("Siguiente"));
    assertFalse(primera.contains("Persona26 "));

    String segunda = escenario.get("/admin/donantes?pagina=2", admin).body();
    assertTrue(segunda.contains("Persona26 Pérez"));
    assertTrue(segunda.contains("Anterior"));
    assertTrue(escenario.get("/admin/donantes?pagina=99", admin).body().contains("página 2 de 2"));
  }

  @Test
  void laAdministracionRegistraUnaPersonaHumanaConCuentaPendiente() throws Exception {
    HttpResponse<String> alta =
        escenario.formulario("/admin/donantes", humana("lucia@example.org", "33444555"), admin);
    assertEquals(302, alta.statusCode(), alta.body());
    assertTrue(alta.headers().firstValue("Location").orElse("").contains("ok=creado"));

    Usuario cuenta = usuario("lucia@example.org");
    assertTrue(cuenta.isPendienteDeActivacion());
    assertTrue(cuenta.getDonanteId().isPresent());
    assertTrue(
        escenario
            .get("/admin/donantes?q=33444555", admin)
            .body()
            .contains("Pendiente de activación"));

    // La bienvenida lleva el enlace para entrar por primera vez, con el correo ya cargado.
    List<String> avisos =
        createQuery("select a.mensaje from AvisoBandeja a where a.donanteId = :id", String.class)
            .setParameter("id", cuenta.getDonanteId().orElseThrow())
            .getResultList();
    assertEquals(1, avisos.size());
    assertTrue(avisos.get(0).contains("/activar?correo=lucia%40example.org"), avisos.get(0));
    assertTrue(
        escenario
            .get("/activar?correo=lucia%40example.org", admin)
            .body()
            .contains("value=\"lucia@example.org\""));
  }

  @Test
  void laAdministracionRegistraUnaPersonaJuridica() throws Exception {
    Map<String, String> campos =
        Map.of(
            "correo", "coop@example.org",
            "donanteTipoPersona", "JURIDICA",
            "donanteCuit", "30-71234567-9",
            "donanteRazonSocial", "Cooperativa Norte",
            "donanteTipoOrganizacion", "ONG",
            "donanteRubro", "Alimentos",
            "donanteMedioContacto", "CORREO");
    assertEquals(302, escenario.formulario("/admin/donantes", campos, admin).statusCode());

    Donante donante = find(Donante.class, usuario("coop@example.org").getDonanteId().orElseThrow());
    assertEquals("30712345679", donante.getNumDocumento());
    assertEquals("Cooperativa Norte", ((PersonajeJuridica) donante).getRazonSocial());
  }

  @Test
  void noSeDuplicanDocumentosNiCorreosYElFormularioConservaLoCargado() throws Exception {
    escenario.crearDonante("Sofía", "30111222", "sofia@example.org");

    HttpResponse<String> documento =
        escenario.formulario("/admin/donantes", humana("otra@example.org", "30111222"), admin);
    assertEquals(400, documento.statusCode());
    assertTrue(
        documento.body().contains("Ya hay una persona donante registrada con ese documento"));
    assertTrue(documento.body().contains("value=\"otra@example.org\""));

    HttpResponse<String> correo =
        escenario.formulario("/admin/donantes", humana("sofia@example.org", "11111111"), admin);
    assertEquals(400, correo.statusCode());
    assertTrue(correo.body().contains("Ya hay una persona donante o una cuenta con ese correo"));
  }

  @Test
  void soloLaAdministracionEntra() throws Exception {
    UUID sofia = escenario.crearDonante("Sofía", "30111222", "sofia@example.org");
    String donante = escenario.sesionDeDonante(sofia, "sofia@example.org");
    assertEquals(403, escenario.get("/admin/donantes", donante).statusCode());
    assertEquals(
        403,
        escenario
            .formulario("/admin/donantes", humana("x@example.org", "12121212"), donante)
            .statusCode());
  }

  // --- Edición y baja ---

  @Test
  void laEdicionAbreConLosDatosCargados() throws Exception {
    UUID sofia = escenario.crearDonante("Sofía", "30111222", "sofia@example.org");

    String pagina = escenario.get("/admin/donantes/" + sofia, admin).body();
    assertTrue(pagina.contains("value=\"Sofía\""));
    assertTrue(pagina.contains("value=\"30111222\""));
    assertTrue(pagina.contains("value=\"sofia@example.org\""));
    assertTrue(pagina.contains("value=\"FEMENINO\" selected"));
    // El tipo de persona no se puede cambiar: la otra opción viene deshabilitada.
    assertTrue(pagina.contains("value=\"JURIDICA\" disabled"));
    assertTrue(escenario.get("/admin/donantes", admin).body().contains("/admin/donantes/" + sofia));
  }

  @Test
  void laAdministracionEditaLosDatosDeUnaPersonaDonante() throws Exception {
    UUID sofia = escenario.crearDonante("Sofía", "30111222", "sofia@example.org");
    Map<String, String> campos = humana("sofia@example.org", "30111222");
    campos.put("donanteNombre", "Sofía Inés");
    campos.put("donanteDireccion", "Av. Siempreviva 742");

    HttpResponse<String> edicion = escenario.formulario("/admin/donantes/" + sofia, campos, admin);
    assertEquals(302, edicion.statusCode(), edicion.body());
    assertTrue(edicion.headers().firstValue("Location").orElse("").contains("ok=editado"));

    PersonaHumana guardada = (PersonaHumana) donante(sofia);
    assertEquals("Sofía Inés", guardada.getNombre());
    assertEquals("Gómez", guardada.getApellido());
    assertEquals("Av. Siempreviva 742", guardada.getDireccion());
    assertEquals(34, guardada.getEdad());
  }

  @Test
  void cambiarElCorreoCambiaTambienElUsuarioConElQueEntra() throws Exception {
    UUID sofia = escenario.crearDonante("Sofía", "30111222", "sofia@example.org");
    escenario.sesionDeDonante(sofia, "sofia@example.org");

    HttpResponse<String> edicion =
        escenario.formulario(
            "/admin/donantes/" + sofia, humana("sofia.nueva@example.org", "30111222"), admin);
    assertEquals(302, edicion.statusCode(), edicion.body());

    assertEquals("sofia.nueva@example.org", donante(sofia).extraerEmail().orElseThrow());
    assertEquals(sofia, usuario("sofia.nueva@example.org").getDonanteId().orElseThrow());
    String sesion = escenario.login("sofia.nueva@example.org", "clave1234");
    assertEquals(200, escenario.get("/donante/donaciones", sesion).statusCode());
  }

  @Test
  void laEdicionNoAceptaDatosDeOtraPersonaNiCambiarElTipo() throws Exception {
    UUID sofia = escenario.crearDonante("Sofía", "30111222", "sofia@example.org");
    escenario.crearDonante("Martín", "28999000", "martin@example.org");
    String ruta = "/admin/donantes/" + sofia;

    HttpResponse<String> correo =
        escenario.formulario(ruta, humana("martin@example.org", "30111222"), admin);
    assertEquals(400, correo.statusCode());
    assertTrue(correo.body().contains("Ya hay una persona donante o una cuenta con ese correo"));

    HttpResponse<String> documento =
        escenario.formulario(ruta, humana("sofia@example.org", "28999000"), admin);
    assertEquals(400, documento.statusCode());
    assertTrue(
        documento.body().contains("Ya hay una persona donante registrada con ese documento"));

    Map<String, String> juridica = new HashMap<>(humana("sofia@example.org", "30111222"));
    juridica.put("donanteTipoPersona", "JURIDICA");
    juridica.put("donanteCuit", "30-71234567-9");
    juridica.put("donanteRazonSocial", "Sofía SA");
    juridica.put("donanteTipoOrganizacion", "EMPRESA");
    juridica.put("donanteRubro", "Varios");
    HttpResponse<String> tipo = escenario.formulario(ruta, juridica, admin);
    assertEquals(400, tipo.statusCode());
    assertTrue(tipo.body().contains("No se puede cambiar el tipo de persona"));

    // Nada de lo rechazado quedó guardado.
    Donante intacta = donante(sofia);
    assertEquals("30111222", intacta.getNumDocumento());
    assertEquals("sofia@example.org", intacta.extraerEmail().orElseThrow());
  }

  @Test
  void laBajaBorraALaPersonaDonanteYSuCuenta() throws Exception {
    UUID sofia = escenario.crearDonante("Sofía", "30111222", "sofia@example.org");
    escenario.sesionDeDonante(sofia, "sofia@example.org");

    HttpResponse<String> baja =
        escenario.formulario("/admin/donantes/" + sofia + "/eliminar", Map.of(), admin);
    assertEquals(302, baja.statusCode(), baja.body());
    assertTrue(baja.headers().firstValue("Location").orElse("").contains("ok=eliminado"));

    entityManager().clear();
    assertEquals(null, find(Donante.class, sofia));
    assertTrue(
        createQuery("select u from Usuario u where u.email = 'sofia@example.org'", Usuario.class)
            .getResultList()
            .isEmpty());
    assertEquals(404, escenario.get("/admin/donantes/" + sofia, admin).statusCode());
  }

  @Test
  void noSeEliminaAQuienTieneDonaciones() throws Exception {
    UUID sofia = escenario.crearDonante("Sofía", "30111222", "sofia@example.org");
    escenario.crearDonacion(sofia, "Arroz");

    HttpResponse<String> baja =
        escenario.formulario("/admin/donantes/" + sofia + "/eliminar", Map.of(), admin);
    assertEquals(409, baja.statusCode());
    assertTrue(baja.body().contains("No se puede eliminar a una persona donante con donaciones"));
    assertTrue(donante(sofia) != null);
  }

  @Test
  void soloLaAdministracionEditaOElimina() throws Exception {
    UUID sofia = escenario.crearDonante("Sofía", "30111222", "sofia@example.org");
    String donante = escenario.sesionDeDonante(sofia, "sofia@example.org");
    assertEquals(403, escenario.get("/admin/donantes/" + sofia, donante).statusCode());
    assertEquals(
        403,
        escenario
            .formulario("/admin/donantes/" + sofia + "/eliminar", Map.of(), donante)
            .statusCode());
    assertEquals(404, escenario.get("/admin/donantes/no-es-un-id", admin).statusCode());
    assertEquals(404, escenario.get("/admin/donantes/" + UUID.randomUUID(), admin).statusCode());
  }

  private Donante donante(UUID id) {
    entityManager().clear();
    return find(Donante.class, id);
  }

  private static Map<String, String> humana(String correo, String documento) {
    Map<String, String> campos = new HashMap<>();
    campos.put("correo", correo);
    campos.put("donanteTipoPersona", "HUMANA");
    campos.put("donanteNombre", "Lucía");
    campos.put("donanteApellido", "Gómez");
    campos.put("donanteEdad", "34");
    campos.put("donanteTipoDocumento", "DNI");
    campos.put("donanteNumeroDocumento", documento);
    campos.put("donanteGenero", "FEMENINO");
    campos.put("donanteDireccion", "Calle 123");
    campos.put("donanteMedioContacto", "CORREO");
    return campos;
  }

  private Usuario usuario(String email) {
    entityManager().clear();
    return createQuery("select u from Usuario u where u.email = :email", Usuario.class)
        .setParameter("email", email)
        .getSingleResult();
  }
}
