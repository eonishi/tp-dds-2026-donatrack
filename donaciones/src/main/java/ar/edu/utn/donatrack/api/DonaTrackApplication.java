package ar.edu.utn.donatrack.api;

public final class DonaTrackApplication {
  private DonaTrackApplication() {}

  public static void main(String[] args) {
    int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
    ConfiguracionBase.desdeEntorno();
    if ("true".equalsIgnoreCase(System.getenv("DONATRACK_DEMO_DATA"))) {
      new ar.edu.utn.donatrack.demo.DemoDataset()
          .initialize(
              System.getenv(),
              new ar.edu.utn.donatrack.security.AuthService(),
              java.time.Clock.systemDefaultZone());
    }
    DonacionesApi.create().start(port);
  }
}
