package ar.edu.utn.donatrack.logistica.controllers;

import ar.edu.utn.donatrack.logistica.Camion;
import ar.edu.utn.donatrack.logistica.CamionRepository;
import ar.edu.utn.donatrack.logistica.Ruta;
import ar.edu.utn.donatrack.logistica.RutaRepository;
import ar.edu.utn.donatrack.logistica.monitoreo.Coordenada;
import ar.edu.utn.donatrack.logistica.monitoreo.LecturaPosicion;
import ar.edu.utn.donatrack.logistica.monitoreo.MonitorRutas;
import io.javalin.http.Context;
import java.time.LocalDateTime;
import java.util.Optional;

// Será llamado por el usuario y no por MS donaciones
public class PosicionController {

  private final MonitorRutas monitor;
  private final CamionRepository camionRepo;
  private final RutaRepository rutaRepo;

  public PosicionController(
      MonitorRutas monitor, CamionRepository camionRepo, RutaRepository rutaRepo) {
    this.monitor = monitor;
    this.camionRepo = camionRepo;
    this.rutaRepo = rutaRepo;
  }

  // POST /logistica/camiones/{patente}/posicion (Ingesta desde el GPS/App)
  public void ingestar(Context ctx) {
    String patente = ctx.pathParam("patente");
    Optional<Camion> camionOpt = camionRepo.buscarPorPatente(patente);

    if (camionOpt.isEmpty()) {
      ctx.status(404).result("Camión no encontrado");
      return;
    }

    try {
      IngestaDTO dto = ctx.bodyAsClass(IngestaDTO.class);

      Coordenada coordenada = new Coordenada(dto.lat, dto.lng);

      LecturaPosicion lectura =
          new LecturaPosicion(camionOpt.get(), coordenada, dto.velocidad, dto.timestamp);

      // El monitor decide si la procesa o la descarta
      monitor.procesar(lectura);

      // 202 Accepted indica que recibimos el paquete
      ctx.status(202).result("Lectura procesada");

    } catch (Exception e) {
      ctx.status(400).result("Error en los datos de la lectura: " + e.getMessage());
    }
  }

  // GET /logistica/camiones/{patente}/posicion (Publica para usuario)
  public void leerPosicion(Context ctx) {
    String patente = ctx.pathParam("patente");
    Optional<Camion> camionOpt = camionRepo.buscarPorPatente(patente);

    if (camionOpt.isEmpty()) {
      ctx.status(404).result("Camión no encontrado");
      return;
    }

    Optional<LecturaPosicion> lecturaOpt = monitor.ultimaPosicion(camionOpt.get());
    if (lecturaOpt.isEmpty()) {
      ctx.status(404).result("No hay posiciones registradas para este camión");
      return;
    }

    LecturaPosicion lectura = lecturaOpt.get();

    // Buscamos la ruta activa
    Optional<Ruta> rutaActiva = rutaRepo.buscarPorCamion(camionOpt.get());
    String idRuta = rutaActiva.map(r -> r.getId().toString()).orElse(null);

    // Transformamos tu record a un DTO para que Jackson use "lat" y "lng"
    CoordenadaDTO coordDto =
        new CoordenadaDTO(lectura.coordenada().latitud(), lectura.coordenada().longitud());

    PosicionResponseDTO response =
        new PosicionResponseDTO(patente, idRuta, coordDto, lectura.timestamp());

    ctx.json(response);
  }

  // --- DTOs Internos ---

  public static class IngestaDTO {
    public double lat;
    public double lng;
    public double velocidad;
    public LocalDateTime timestamp;
  }

  public record CoordenadaDTO(double lat, double lng) {}

  public record PosicionResponseDTO(
      String idCamion, String idRutaActiva, CoordenadaDTO coordenadas, LocalDateTime timestamp) {}
}
