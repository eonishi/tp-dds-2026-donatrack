package ar.edu.utn.donatrack.logistica.demo;

import ar.edu.utn.donatrack.logistica.Camion;
import ar.edu.utn.donatrack.logistica.CamionRepository;
import ar.edu.utn.donatrack.logistica.Repartidor;
import ar.edu.utn.donatrack.logistica.RepartidorRepository;
import ar.edu.utn.donatrack.logistica.Ruta;
import ar.edu.utn.donatrack.logistica.RutaRepository;
import ar.edu.utn.donatrack.logistica.monitoreo.Coordenada;
import ar.edu.utn.donatrack.logistica.monitoreo.LecturaPosicion;
import ar.edu.utn.donatrack.logistica.monitoreo.MonitorRutas;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Optional local fleet fixtures; never dispatches remote donation callbacks. */
public class LogisticsDemoDataset implements WithSimplePersistenceUnit {
  static final String VERSION = "logistics-demo-v1";
  private final CamionRepository camiones = new CamionRepository();
  private final RutaRepository rutas = new RutaRepository();
  private final RepartidorRepository repartidores = new RepartidorRepository();

  public void initialize(Map<String, String> environment, MonitorRutas monitor, Clock clock) {
    if (!"true".equalsIgnoreCase(environment.get("DONATRACK_DEMO_DATA"))) {
      return;
    }
    withTransaction(
        () -> {
          if (find(LogisticsDemoVersion.class, VERSION) != null) {
            return;
          }
          for (String patente : List.of("DEMO-01", "DEMO-02", "DEMO-03")) {
            if (camiones.buscarPorPatente(patente).isPresent()) {
              throw new IllegalStateException("Demo logistics plate collision: " + patente);
            }
          }
          for (String email :
              List.of("demo.chofer@example.test", "demo.acompanante@example.test")) {
            if (repartidores.buscarPorEmail(email).isPresent()) {
              throw new IllegalStateException("Demo logistics account collision: " + email);
            }
          }
          Camion urbano = new Camion("DEMO-01", 18, 2.6, 3500);
          Camion grande = new Camion("DEMO-02", 40, 3.4, 10000);
          Camion libre = new Camion("DEMO-03", 10, 2.3, 1500);
          List.of(urbano, grande, libre).forEach(camiones::agregar);
          Repartidor chofer =
              new Repartidor("Demo Driver", "demo.chofer@example.test", "Demo12345!");
          Repartidor acompanante =
              new Repartidor("Demo Assistant", "demo.acompanante@example.test", "Demo12345!");
          repartidores.agregar(chofer);
          repartidores.agregar(acompanante);
          // Empty training routes intentionally avoid fabricated cross-service donation/entity IDs.
          Ruta activa = new Ruta(urbano, List.of());
          activa.asignarRepartidor(chofer);
          activa.asignarRepartidor(acompanante);
          activa.iniciar();
          rutas.agregar(activa);
          Ruta pendiente = new Ruta(grande, List.of());
          pendiente.asignarRepartidor(chofer);
          rutas.agregar(pendiente);
          persist(new LogisticsDemoVersion(VERSION));
        });
    // Positions live in memory. Restore only a simulated reading for our still-active demo truck.
    camiones
        .buscarPorPatente("DEMO-01")
        .filter(c -> rutas.buscarPorCamion(c).isPresent())
        .ifPresent(
            c ->
                monitor.procesar(
                    new LecturaPosicion(
                        c, new Coordenada(-34.6037, -58.3816), 0, LocalDateTime.now(clock))));
  }
}
