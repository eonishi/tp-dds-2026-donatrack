package ar.edu.utn.donatrack.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.beneficiarios.Necesidad;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.EstadoDonacion;
import ar.edu.utn.donatrack.security.AuthService;
import ar.edu.utn.donatrack.security.Role;
import ar.edu.utn.donatrack.security.Usuario;
import ar.edu.utn.donatrack.security.UsuariosRepository;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.time.Clock;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DemoDatasetTest implements WithSimplePersistenceUnit {
  private final AuthService auth = new AuthService();
  private final DemoDataset dataset = new DemoDataset();

  @Test
  void disabledByDefault() {
    dataset.initialize(Map.of(), auth, Clock.systemUTC());
    assertNull(find(DemoDatasetVersion.class, DemoDataset.VERSION));
    assertTrue(DemoDataset.EMAILS.stream().noneMatch(auth::existeUsuario));
  }

  @Test
  void coversStatesAndLinkedRolesWithoutResettingOnRestart() {
    seed();
    entityManager().clear();
    List<Donacion> donations =
        createQuery(
                "select d from Donacion d where d.donante.numDocumento in ('99000001', '30999999991')",
                Donacion.class)
            .getResultList();
    assertEquals(22, donations.size());
    assertEquals(
        EnumSet.allOf(EstadoDonacion.class),
        EnumSet.copyOf(donations.stream().map(Donacion::getEstado).toList()));
    assertTrue(
        donations.stream()
            .allMatch(
                d ->
                    d.getBienes().stream()
                        .allMatch(b -> b.getSubcategoria().equals(d.getSubcategoria()))));
    Usuario human =
        auth.authenticate(DemoDataset.EMAILS.get(1), DemoDataset.PASSWORD).orElseThrow();
    Usuario legal =
        auth.authenticate(DemoDataset.EMAILS.get(2), DemoDataset.PASSWORD).orElseThrow();
    Usuario beneficiary =
        auth.authenticate(DemoDataset.EMAILS.get(3), DemoDataset.PASSWORD).orElseThrow();
    assertTrue(
        auth.authenticate(DemoDataset.EMAILS.get(0), DemoDataset.PASSWORD)
            .orElseThrow()
            .hasRole(Role.ADMIN));
    assertNotEquals(human.getDonanteId(), legal.getDonanteId());
    for (Usuario donor : List.of(human, legal)) {
      assertEquals(
          EnumSet.allOf(EstadoDonacion.class),
          EnumSet.copyOf(
              donations.stream()
                  .filter(d -> donor.getDonanteId().orElseThrow().equals(d.getDonante().getId()))
                  .map(Donacion::getEstado)
                  .toList()));
      assertTrue(
          createQuery("select count(a) from AvisoBandeja a where a.donanteId = :id", Long.class)
                  .setParameter("id", donor.getDonanteId().orElseThrow())
                  .getSingleResult()
              >= 4);
    }
    assertTrue(
        donations.stream()
            .anyMatch(d -> human.getDonanteId().orElseThrow().equals(d.getDonante().getId())));
    EntidadBeneficiaria entity =
        find(EntidadBeneficiaria.class, beneficiary.getEntidadId().orElseThrow());
    assertEquals(7, entity.necesidades().size());
    assertTrue(entity.necesidades().stream().anyMatch(Necesidad::estaSatisfecha));
    assertTrue(entity.necesidades().stream().anyMatch(n -> n.getCantidadRecibida() == 0));
    assertTrue(
        entity.necesidades().stream()
            .anyMatch(n -> n.getCantidadRecibida() > 0 && !n.estaSatisfecha()));
    Donacion inDeposit =
        donations.stream()
            .filter(d -> d.getEstado() == EstadoDonacion.EN_DEPOSITO)
            .findFirst()
            .orElseThrow();
    withTransaction(inDeposit::marcarVencida);
    seed();
    entityManager().clear();
    assertEquals(EstadoDonacion.VENCIDA, find(Donacion.class, inDeposit.getId()).getEstado());
    assertEquals(
        22L,
        createQuery(
                "select count(d) from Donacion d where d.donante.numDocumento in ('99000001', '30999999991')",
                Long.class)
            .getSingleResult());
  }

  @Test
  void collisionDoesNotOverwriteExistingAccountOrCommitPartialData() {
    auth.registrar(
        DemoDataset.EMAILS.get(1), "ExistingPassword", java.util.Set.of(Role.DONANTE), null, null);
    assertThrows(IllegalStateException.class, this::seed);
    entityManager().clear();
    assertTrue(auth.authenticate(DemoDataset.EMAILS.get(1), "ExistingPassword").isPresent());
    assertFalse(auth.existeUsuario(DemoDataset.EMAILS.get(0)));
    assertNull(find(DemoDatasetVersion.class, DemoDataset.VERSION));
  }

  @Test
  void catalogCollisionRollsBackPartiallyCreatedFixtures() {
    withTransaction(
        () ->
            persist(
                new ar.edu.utn.donatrack.bienes.SubCategoria(
                    "[DEMO] Sillas", ar.edu.utn.donatrack.bienes.Categoria.ALIMENTO)));
    assertThrows(IllegalStateException.class, this::seed);
    entityManager().clear();
    assertTrue(
        new ar.edu.utn.donatrack.bienes.SubcategoriasRepository()
            .buscarPorNombre("[DEMO] Arroz")
            .isEmpty());
    assertTrue(DemoDataset.EMAILS.stream().noneMatch(auth::existeUsuario));
    assertNull(find(DemoDatasetVersion.class, DemoDataset.VERSION));
  }

  @Test
  void upgradesExistingV1WithoutReplacingItsRecords() throws Exception {
    var originalSeed = DemoDataset.class.getDeclaredMethod("seed", AuthService.class, Clock.class);
    originalSeed.setAccessible(true);
    withTransaction(
        () -> {
          try {
            originalSeed.invoke(dataset, auth, Clock.systemUTC());
          } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
          }
          persist(new DemoDatasetVersion(DemoDataset.VERSION));
        });
    List<Donacion> originals =
        createQuery("select d from Donacion d", Donacion.class).getResultList();
    var originalIds = originals.stream().map(Donacion::getId).toList();
    seed();
    entityManager().clear();
    assertTrue(originalIds.stream().allMatch(id -> find(Donacion.class, id) != null));
    assertEquals(
        22L,
        createQuery(
                "select count(d) from Donacion d where d.donante.numDocumento in ('99000001', '30999999991')",
                Long.class)
            .getSingleResult());
    seed();
    assertEquals(
        22L,
        createQuery(
                "select count(d) from Donacion d where d.donante.numDocumento in ('99000001', '30999999991')",
                Long.class)
            .getSingleResult());
  }

  @Test
  void populatedAccountsCanBrowseEveryRoleView() throws Exception {
    seed();
    var app =
        ar.edu.utn.donatrack.testsupport.ApiDePrueba.crear(
                Clock.systemUTC(),
                plate ->
                    java.util.Optional.of(
                        new ar.edu.utn.donatrack.seguimiento.PosicionCamion(
                            plate, -34.5985, -58.4201, "2026-11-07T10:15:00")))
            .start(0);
    try {
      String base = "http://localhost:" + app.port();
      for (int account = 1; account <= 3; account++) {
        var cookies = new java.net.CookieManager();
        var client = java.net.http.HttpClient.newBuilder().cookieHandler(cookies).build();
        var login =
            client.send(
                java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/login"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(
                        java.net.http.HttpRequest.BodyPublishers.ofString(
                            "email=" + EMAIL(account) + "&password=Demo12345%21"))
                    .build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(302, login.statusCode());
        String prefix = account == 3 ? "/entidad" : "/donante";
        List<String> paths =
            new java.util.ArrayList<>(
                List.of(
                    "/dashboard",
                    prefix + "/seguimiento",
                    prefix + "/notificaciones",
                    prefix + "/seguimiento/posiciones"));
        paths.add(account == 3 ? "/entidad/necesidades" : "/donante/donaciones");
        if (account == 3) paths.add("/entidad/recepciones");
        for (String path : paths) {
          var response =
              client.send(
                  java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + path))
                      .GET()
                      .build(),
                  java.net.http.HttpResponse.BodyHandlers.ofString());
          assertEquals(200, response.statusCode(), path);
          if (path.endsWith("/posiciones")) {
            assertTrue(response.body().contains("DEMO-01"), path);
            assertTrue(response.body().contains("-34.5985"), path);
          } else if (!path.equals("/dashboard")) {
            assertTrue(response.body().contains("[DEMO]"), path);
          }
        }
      }
    } finally {
      app.stop();
    }
  }

  private static String EMAIL(int index) {
    return java.net.URLEncoder.encode(
        DemoDataset.EMAILS.get(index), java.nio.charset.StandardCharsets.UTF_8);
  }

  private void seed() {
    dataset.initialize(Map.of("DONATRACK_DEMO_DATA", "true"), auth, Clock.systemUTC());
  }

  @AfterEach
  void cleanup() {
    entityManager().clear();
    withTransaction(
        () -> {
          createQuery(
                  "select p from PropuestaDeAsignacion p where p.donacion.donante.numDocumento in ('99000001', '30999999991')",
                  ar.edu.utn.donatrack.propuestas.PropuestaDeAsignacion.class)
              .getResultList()
              .forEach(this::remove);
          createQuery(
                  "select a from AvisoBandeja a where a.mensaje like '[DEMO]%'",
                  ar.edu.utn.donatrack.notificaciones.AvisoBandeja.class)
              .getResultList()
              .forEach(this::remove);
          createQuery(
                  "select d from Donacion d where d.donante.numDocumento in ('99000001', '30999999991')",
                  Donacion.class)
              .getResultList()
              .forEach(this::remove);
          entityManager().flush();
          for (String email : DemoDataset.EMAILS) {
            new UsuariosRepository().buscarPorEmail(email).ifPresent(this::remove);
          }
          createQuery(
                  "select e from EntidadBeneficiaria e where e.razonSocial = '[DEMO] Comedor Encuentro'",
                  EntidadBeneficiaria.class)
              .getResultList()
              .forEach(this::remove);
          createQuery(
                  "select d from Donante d where d.numDocumento in ('99000001', '30999999991')",
                  ar.edu.utn.donatrack.donantes.Donante.class)
              .getResultList()
              .forEach(this::remove);
          entityManager().flush();
          createQuery(
                  "select s from SubCategoria s where s.nombre like '[DEMO]%'",
                  ar.edu.utn.donatrack.bienes.SubCategoria.class)
              .getResultList()
              .forEach(this::remove);
          DemoDatasetVersion marker = find(DemoDatasetVersion.class, DemoDataset.VERSION);
          if (marker != null) remove(marker);
          DemoDatasetVersion viewsMarker =
              find(DemoDatasetVersion.class, DemoDataset.VIEWS_VERSION);
          if (viewsMarker != null) remove(viewsMarker);
        });
    entityManager().clear();
  }
}
