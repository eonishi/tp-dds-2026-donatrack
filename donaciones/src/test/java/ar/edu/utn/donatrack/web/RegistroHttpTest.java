package ar.edu.utn.donatrack.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.donatrack.api.DonacionesApi;
import ar.edu.utn.donatrack.api.ServiciosExternos;
import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.donantes.PersonaHumana;
import ar.edu.utn.donatrack.donantes.PersonajeJuridica;
import ar.edu.utn.donatrack.donantes.Sms;
import ar.edu.utn.donatrack.donantes.TipoDocumento;
import ar.edu.utn.donatrack.donantes.TipoOrganizacion;
import ar.edu.utn.donatrack.donantes.Whatsapp;
import ar.edu.utn.donatrack.notificaciones.BandejaNotificacionesRepository;
import ar.edu.utn.donatrack.notificaciones.EstadoNotificacion;
import ar.edu.utn.donatrack.notificaciones.NotificacionesRepository;
import ar.edu.utn.donatrack.notificaciones.NotificadorSimulado;
import ar.edu.utn.donatrack.notificaciones.ServicioNotificaciones;
import ar.edu.utn.donatrack.security.Usuario;
import ar.edu.utn.donatrack.testsupport.ApiDePrueba;
import ar.edu.utn.donatrack.testsupport.BaseDePrueba;
import ar.edu.utn.donatrack.testsupport.FlotaEnMemoria;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.Javalin;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** El registro crea la persona donante o la entidad junto con su usuario ya vinculado. */
class RegistroHttpTest implements WithSimplePersistenceUnit {
  private final ObjectMapper mapper = new ObjectMapper();
  private final HttpClient client = HttpClient.newHttpClient();
  private Javalin app;
  private String baseUrl;
  private NotificacionesRepository notificaciones;

  @BeforeEach
  void start() {
    notificaciones = new NotificacionesRepository();
    Clock clock = Clock.systemUTC();
    app =
        DonacionesApi.create(
                clock,
                new ServicioNotificaciones(
                    new NotificadorSimulado(),
                    notificaciones,
                    1,
                    new BandejaNotificacionesRepository(clock)),
                new ServiciosExternos(
                    patente -> Optional.empty(),
                    new FlotaEnMemoria(),
                    direccion -> Optional.of(ApiDePrueba.UBICACION)))
            .start(0);
    baseUrl = "http://localhost:" + app.port();
  }

  @AfterEach
  void stop() {
    app.stop();
  }

  @AfterEach
  void limpiarPersistencia() {
    BaseDePrueba.vaciar();
  }

  @Test
  void laDonanteSeRegistraQuedaLogueadaYVeSoloSusDonaciones() throws Exception {
    HttpResponse<String> registro = registrar(donante("ana@example.org", "30111222"));

    assertEquals(302, registro.statusCode(), registro.body());
    assertEquals("/dashboard", registro.headers().firstValue("Location").orElse(""));
    String cookieAna = cookie(registro);

    String admin = cookie(login("admin@donatrack.local", "admin123"));
    String donanteId = buscarDonante(admin, "30111222").get("id").asText();
    assertEquals(201, crearDonacion(admin, donanteId).statusCode());

    HttpResponse<String> deAna = get("/api/v1/donaciones", cookieAna);
    assertEquals(200, deAna.statusCode());
    assertEquals(1, mapper.readTree(deAna.body()).size(), "ve la donación que se le cargó");
    assertEquals(
        302, login("ana@example.org", "clave-segura").statusCode(), "puede volver a entrar");
  }

  @Test
  void laEntidadSeRegistraYQuedaLogueada() throws Exception {
    Map<String, String> campos = comunes("comedor@example.org");
    campos.put("rol", "entidad");
    campos.put("entidadRazonSocial", "Comedor Los Pibes");
    campos.put("entidadDireccion", "Calle 123");
    campos.put("entidadTelefono", "1155555555");

    HttpResponse<String> registro = registrar(campos);

    assertEquals(302, registro.statusCode(), registro.body());
    assertEquals(200, get("/dashboard", cookie(registro)).statusCode());
  }

  @Test
  void noSePuedeRegistrarDosVecesElMismoCorreo() throws Exception {
    assertEquals(302, registrar(donante("ana@example.org", "30111222")).statusCode());

    HttpResponse<String> repetido = registrar(donante("ana@example.org", "30999888"));

    assertEquals(400, repetido.statusCode());
    assertTrue(repetido.body().contains("Ya hay una cuenta con ese correo electrónico."));
  }

  @Test
  void lasContraseniasTienenQueCoincidir() throws Exception {
    Map<String, String> campos = donante("ana@example.org", "30111222");
    campos.put("contrasenaRepetida", "otra-clave");

    HttpResponse<String> registro = registrar(campos);

    assertEquals(400, registro.statusCode());
    assertTrue(registro.body().contains("Las contraseñas no coinciden."));
  }

  @Test
  void persistsHumanAgeAndSendsWelcomeToPreferredContact() throws Exception {
    String[] channels = {"CORREO", "SMS", "WHATSAPP"};
    Class<?>[] contacts = {CorreoElectronico.class, Sms.class, Whatsapp.class};
    for (int i = 0; i < channels.length; i++) {
      String email = "human" + i + "@example.org";
      Map<String, String> fields = donante(email, "4011122" + i);
      fields.put("donanteTelefono", "+5491155555555");
      fields.put("donanteMedioContacto", channels[i]);
      assertEquals(302, registrar(fields).statusCode());
      entityManager().clear();
      PersonaHumana human =
          (PersonaHumana)
              new ar.edu.utn.donatrack.donantes.DonantesRepository()
                  .getDonantes().stream()
                      .filter(d -> d.extraerEmail().orElse("").equals(email))
                      .findFirst()
                      .orElseThrow();
      assertEquals(35, human.getEdad());
      var welcome = notificaciones.getNotificaciones().get(i);
      assertInstanceOf(contacts[i], welcome.getDestinatario());
      assertEquals(i == 0 ? email : "+5491155555555", welcome.getDestinatario().getContacto());
      assertEquals(EstadoNotificacion.ENVIADA, welcome.getEstado());
      assertTrue(welcome.getMensaje().contains("Gracias por formar parte del proyecto"));
      assertTrue(welcome.getMensaje().contains(baseUrl + "/login"));
      assertEquals(human.getId(), ((Donante) welcome.getTitular().orElseThrow()).getId());
    }
  }

  @Test
  void registersLegalDonorsWithoutHumanFields() throws Exception {
    for (TipoOrganizacion type : TipoOrganizacion.values()) {
      Map<String, String> fields = legal("org" + type.name().toLowerCase() + "@example.org");
      fields.put("donanteCuit", "30-1234567" + type.ordinal() + "-9");
      fields.put("donanteTipoOrganizacion", type.name());
      fields.put("donanteTelefono", "+5491155555555");
      fields.put("donanteMedioContacto", "WHATSAPP");
      assertEquals(302, registrar(fields).statusCode());
      entityManager().clear();
      PersonajeJuridica legal =
          (PersonajeJuridica)
              new ar.edu.utn.donatrack.donantes.DonantesRepository()
                  .getDonantes().stream()
                      .filter(d -> d.extraerEmail().orElse("").equals(fields.get("correo")))
                      .findFirst()
                      .orElseThrow();
      assertEquals("Fundación Test", legal.getRazonSocial());
      assertEquals("Educación", legal.getRubro());
      assertEquals(type, legal.getTipoOrganizacion());
      assertEquals(TipoDocumento.CUIT, legal.getDocumento());
      assertEquals(fields.get("donanteCuit").replace("-", ""), legal.getNumDocumento());
      assertInstanceOf(
          Whatsapp.class, notificaciones.getNotificaciones().get(type.ordinal()).getDestinatario());
    }
  }

  @Test
  void rejectsMissingHumanFieldsWithoutCreatingAccountsOrWelcomes() throws Exception {
    for (String field :
        new String[] {
          "correo",
          "donanteTipoPersona",
          "donanteNombre",
          "donanteApellido",
          "donanteEdad",
          "donanteTipoDocumento",
          "donanteNumeroDocumento",
          "donanteGenero",
          "donanteDireccion",
          "donanteMedioContacto"
        }) {
      Map<String, String> fields = donante("missing@example.org", "50111222");
      fields.remove(field);
      assertEquals(400, registrar(fields).statusCode(), field);
    }
    assertTrue(notificaciones.getNotificaciones().isEmpty());
    assertTrue(
        createQuery("select u from Usuario u where u.email = 'missing@example.org'", Usuario.class)
            .getResultList()
            .isEmpty());
  }

  @Test
  void rejectsMissingLegalFieldsAndInvalidValues() throws Exception {
    for (String field :
        new String[] {
          "donanteCuit", "donanteRazonSocial", "donanteTipoOrganizacion", "donanteRubro"
        }) {
      Map<String, String> fields = legal("missing@example.org");
      fields.remove(field);
      assertEquals(400, registrar(fields).statusCode(), field);
    }
    for (String value : new String[] {"abc", "1234", "301234567890"}) {
      Map<String, String> fields = legal("badcuit@example.org");
      fields.put("donanteCuit", value);
      assertEquals(400, registrar(fields).statusCode());
    }
    for (String age : new String[] {"-1", "131", "2.5", "abc"}) {
      Map<String, String> fields = donante("age@example.org", "50111222");
      fields.put("donanteEdad", age);
      assertEquals(400, registrar(fields).statusCode());
    }
    assertTrue(notificaciones.getNotificaciones().isEmpty());
  }

  @Test
  void requiresEmailEvenWithPreferredPhoneAndRejectsUnavailableContact() throws Exception {
    for (String channel : new String[] {"SMS", "WHATSAPP", "INVALID"}) {
      Map<String, String> fields = donante("phone@example.org", "50111222");
      fields.put("donanteMedioContacto", channel);
      assertEquals(400, registrar(fields).statusCode());
      fields.put("donanteTelefono", "+5491155555555");
      fields.remove("correo");
      assertEquals(400, registrar(fields).statusCode());
    }
    assertTrue(notificaciones.getNotificaciones().isEmpty());
  }

  @Test
  void rejectsMalformedPhonesBeforeCreatingAccountsOrWelcomes() throws Exception {
    for (String channel : new String[] {"CORREO", "SMS", "WHATSAPP"}) {
      for (String phone :
          new String[] {"abc", "1155555555", "+54 91155555555", "+0123456", "+1234567890123456"}) {
        Map<String, String> fields = donante("badphone@example.org", "60111222");
        fields.put("donanteTelefono", phone);
        fields.put("donanteMedioContacto", channel);
        assertEquals(400, registrar(fields).statusCode(), phone);
      }
    }
    assertTrue(notificaciones.getNotificaciones().isEmpty());
    assertTrue(
        createQuery("select u from Usuario u where u.email = 'badphone@example.org'", Usuario.class)
            .getResultList()
            .isEmpty());
  }

  @Test
  void publicPagesExposeRegistrationAndNotActivation() throws Exception {
    assertTrue(get("/", "").body().contains("href=\"/registro\">Registrarse</a>"));
    assertFalse(get("/login", "").body().contains("Activá tu cuenta"));
    String registration = get("/registro", "").body();
    for (String name :
        new String[] {
          "donanteEdad", "donanteCuit", "donanteTipoPersona", "donanteRazonSocial", "donanteRubro"
        }) {
      assertTrue(registration.contains("name=\"" + name + "\""));
    }
    assertTrue(registration.contains("/registro.js"));
    assertTrue(get("/registro.js", "").body().contains("input.disabled = !selected"));
  }

  private Map<String, String> legal(String email) {
    Map<String, String> fields = comunes(email);
    fields.put("rol", "donante");
    fields.put("donanteTipoPersona", "JURIDICA");
    fields.put("donanteCuit", "30-12345678-9");
    fields.put("donanteRazonSocial", "Fundación Test");
    fields.put("donanteTipoOrganizacion", "ONG");
    fields.put("donanteRubro", "Educación");
    fields.put("donanteMedioContacto", "CORREO");
    return fields;
  }

  private Map<String, String> comunes(String correo) {
    Map<String, String> campos = new LinkedHashMap<>();
    campos.put("correo", correo);
    campos.put("contrasena", "clave-segura");
    campos.put("contrasenaRepetida", "clave-segura");
    campos.put("terminos", "acepto");
    return campos;
  }

  private Map<String, String> donante(String correo, String documento) {
    Map<String, String> campos = comunes(correo);
    campos.put("rol", "donante");
    campos.put("donanteTipoPersona", "HUMANA");
    campos.put("donanteEdad", "35");
    campos.put("donanteNombre", "Ana");
    campos.put("donanteApellido", "Gómez");
    campos.put("donanteTipoDocumento", "DNI");
    campos.put("donanteNumeroDocumento", documento);
    campos.put("donanteGenero", "FEMENINO");
    campos.put("donanteDireccion", "Calle 123");
    campos.put("donanteMedioContacto", "CORREO");
    return campos;
  }

  private JsonNode buscarDonante(String cookie, String documento) throws Exception {
    for (JsonNode donante : mapper.readTree(get("/api/v1/donantes", cookie).body())) {
      if (documento.equals(donante.get("numeroDocumento").asText())) {
        return donante;
      }
    }
    throw new AssertionError("no se creó la persona donante " + documento);
  }

  private HttpResponse<String> crearDonacion(String cookie, String donanteId) throws Exception {
    String body =
        "{\"donanteId\":\""
            + donanteId
            + "\",\"bienes\":[{\"descripcion\":\"Arroz\",\"cantidad\":5,\"unidad\":\"KILOGRAMOS\","
            + "\"estadoDeUso\":\"NO_APLICA\",\"subcategoria\":\"Arroz\",\"categoria\":\"ALIMENTO\"}]}";
    return client.send(
        HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/donaciones"))
            .header("Content-Type", "application/json")
            .header("Cookie", cookie)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> registrar(Map<String, String> campos) throws Exception {
    return postFormulario("/registro", campos);
  }

  private HttpResponse<String> login(String email, String password) throws Exception {
    return postFormulario("/login", Map.of("email", email, "password", password));
  }

  private HttpResponse<String> postFormulario(String ruta, Map<String, String> campos)
      throws Exception {
    String body =
        campos.entrySet().stream()
            .map(
                e ->
                    URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                        + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));
    return client.send(
        HttpRequest.newBuilder(URI.create(baseUrl + ruta))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> get(String ruta, String cookie) throws Exception {
    return client.send(
        HttpRequest.newBuilder(URI.create(baseUrl + ruta)).header("Cookie", cookie).GET().build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private static String cookie(HttpResponse<String> response) {
    String setCookie = response.headers().firstValue("Set-Cookie").orElseThrow();
    return setCookie.substring(0, setCookie.indexOf(';'));
  }
}
