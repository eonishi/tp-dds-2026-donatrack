package ar.edu.utn.donatrack.ubicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.Javalin;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class UbicadorEntidadesTest implements WithSimplePersistenceUnit {

  @AfterEach
  void limpiar() {
    withTransaction(
        () ->
            createQuery("select e from EntidadBeneficiaria e", EntidadBeneficiaria.class)
                .getResultList()
                .forEach(this::remove));
  }

  @Test
  void ubicaLasEntidadesPendientesYNoReintentaLasQueNoEncuentra() {
    Long encontrada = guardar("Comedor", "Av. Medrano 951, CABA");
    Long perdida = guardar("Merendero", "Calle que no existe 0");
    List<String> consultadas = new ArrayList<>();
    Geocodificador geocodificador =
        direccion -> {
          consultadas.add(direccion);
          return direccion.startsWith("Av. Medrano")
              ? Optional.of(new Coordenadas(-34.598, -58.420))
              : Optional.empty();
        };
    UbicadorEntidades ubicador = new UbicadorEntidades(geocodificador, 0);

    assertEquals(1, ubicador.ubicarPendientes());
    assertEquals(0, ubicador.ubicarPendientes(), "la segunda pasada no repite");

    entityManager().clear();
    assertEquals(-34.598, find(EntidadBeneficiaria.class, encontrada).getLatitud().orElseThrow());
    assertFalse(find(EntidadBeneficiaria.class, perdida).estaUbicada());
    assertEquals(2, consultadas.size());
  }

  @Test
  void siElServicioNoRespondeSeReintentaEnLaProximaPasada() {
    Long id = guardar("Comedor", "Av. Medrano 951, CABA");
    new UbicadorEntidades(
            direccion -> {
              throw new GeocodificacionNoDisponibleException("caído", null);
            },
            0)
        .ubicarPendientes();

    entityManager().clear();
    assertTrue(find(EntidadBeneficiaria.class, id).faltaUbicar());
  }

  @Test
  void cambiarLaDireccionObligaAUbicarDeNuevo() {
    EntidadBeneficiaria entidad = new EntidadBeneficiaria("Comedor", "Calle 1", "11");
    entidad.ubicar(-34.6, -58.4);

    entidad.actualizarDatos("Comedor", "Calle 2", "11", List.of());

    assertTrue(entidad.faltaUbicar());
    assertFalse(entidad.estaUbicada());
  }

  @Test
  void nominatimSeConsultaPorArgentinaYDevuelveLaPrimeraCoincidencia() {
    List<String> pedidos = new ArrayList<>();
    Javalin nominatim =
        Javalin.create()
            .get(
                "/search",
                ctx -> {
                  pedidos.add(ctx.queryParam("q") + "|" + ctx.queryParam("countrycodes"));
                  pedidos.add(ctx.header("User-Agent"));
                  ctx.contentType("application/json")
                      .result(
                          "Medrano".equals(ctx.queryParam("q"))
                              ? "[{\"lat\":\"-34.598\",\"lon\":\"-58.420\"}]"
                              : "[]");
                })
            .start(0);
    try {
      Geocodificador geocodificador =
          new GeocodificadorNominatim(
              HttpClient.newHttpClient(), "http://localhost:" + nominatim.port());

      assertEquals(
          Optional.of(new Coordenadas(-34.598, -58.420)), geocodificador.ubicar("Medrano"));
      assertEquals(Optional.empty(), geocodificador.ubicar("Nada"));
      assertEquals("Medrano|ar", pedidos.get(0));
      assertTrue(pedidos.get(1).startsWith("DonaTrack/"), "Nominatim exige identificarse");
    } finally {
      nominatim.stop();
    }
  }

  @Test
  void unErrorDeNominatimNoSeConfundeConUnaDireccionInexistente() {
    Javalin caido = Javalin.create().get("/search", ctx -> ctx.status(503)).start(0);
    try {
      Geocodificador geocodificador =
          new GeocodificadorNominatim(
              HttpClient.newHttpClient(), "http://localhost:" + caido.port());
      assertThrows(GeocodificacionNoDisponibleException.class, () -> geocodificador.ubicar("X"));
    } finally {
      caido.stop();
    }
  }

  private Long guardar(String nombre, String direccion) {
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(nombre, direccion, "1111111111");
    withTransaction(() -> persist(entidad));
    return entidad.getId();
  }
}
