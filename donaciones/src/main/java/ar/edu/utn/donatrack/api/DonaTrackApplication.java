package ar.edu.utn.donatrack.api;

public final class DonaTrackApplication {
  private DonaTrackApplication() {}

  public static void main(String[] args) {
    int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
    DonacionesApi.create().start(port);
  }
}
