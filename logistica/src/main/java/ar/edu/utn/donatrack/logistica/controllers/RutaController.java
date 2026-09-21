package ar.edu.utn.donatrack.logistica.controllers;

import ar.edu.utn.donatrack.logistica.Camion;
import ar.edu.utn.donatrack.logistica.CamionRepository;
import ar.edu.utn.donatrack.logistica.Destino;
import ar.edu.utn.donatrack.logistica.Ruta;
import ar.edu.utn.donatrack.logistica.RutaRepository;
import io.javalin.http.Context;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RutaController {

  private final RutaRepository rutaRepo;
  private final CamionRepository camionRepo;

  public RutaController(RutaRepository rutaRepo, CamionRepository camionRepo) {
    this.rutaRepo = rutaRepo;
    this.camionRepo = camionRepo;
  }

  // GET /logistica/rutas
  public void listar(Context ctx) {
    ctx.json(rutaRepo.listarTodas());
  }

  // GET /logistica/rutas/{id}
  public void buscar(Context ctx) {
    try {
      UUID id = UUID.fromString(ctx.pathParam("id"));
      rutaRepo
          .buscarPorId(id)
          .ifPresentOrElse(ctx::json, () -> ctx.status(404).result("Ruta no encontrada"));
    } catch (IllegalArgumentException e) {
      ctx.status(400).result("Formato de UUID inválido");
    }
  }

  // POST /logistica/rutas
  public void crear(Context ctx) {
    RutaDTO dto = ctx.bodyAsClass(RutaDTO.class);

    // 1. Buscamos el camión referenciado en el JSON
    Optional<Camion> camionOpt = camionRepo.buscarPorPatente(dto.patenteCamion);
    if (camionOpt.isEmpty()) {
      ctx.status(400).result("No se puede crear la ruta: Camión inexistente");
      return;
    }

    try {
      // 2. Instanciamos la ruta
      Ruta nuevaRuta = new Ruta(camionOpt.get(), dto.destinos);

      // 3. Guardamos y respondemos
      rutaRepo.agregar(nuevaRuta);
      ctx.status(201).json(nuevaRuta);
    } catch (Exception e) {
      ctx.status(400).result(e.getMessage());
    }
  }

  // // PATCH /api/logistica/rutas/{id}/iniciar
  // public void iniciar(Context ctx) {
  //     UUID id = UUID.fromString(ctx.pathParam("id"));
  //     Optional<Ruta> rutaOpt = rutaRepo.buscarPorId(id);

  //     if (rutaOpt.isEmpty()) {
  //         ctx.status(404).result("Ruta no encontrada");
  //         return;
  //     }

  //     Ruta ruta = rutaOpt.get();
  //     try {
  //         ruta.iniciar(); // Usa tu lógica de dominio

  //         // TODO: (Integración S2S - Issue #9)
  //         // Hacer petición PATCH a /api/donaciones/estado para pasar
  //         // las donaciones a EN_TRASLADO y enviar el idCamion.
  //         ctx.status(200).json(ruta);
  //     } catch (IllegalStateException e) {
  //         ctx.status(400).result(e.getMessage());
  //     }
  // }

  // // PATCH /api/logistica/rutas/{id}/finalizar
  // public void finalizar(Context ctx) {
  //     UUID id = UUID.fromString(ctx.pathParam("id"));
  //     Optional<Ruta> rutaOpt = rutaRepo.buscarPorId(id);

  //     if (rutaOpt.isEmpty()) {
  //         ctx.status(404).result("Ruta no encontrada");
  //         return;
  //     }

  //     Ruta ruta = rutaOpt.get();
  //     try {
  //         ruta.finalizar(); // Usa tu lógica de dominio
  //         ctx.status(200).json(ruta);
  //     } catch (IllegalStateException e) {
  //         ctx.status(400).result(e.getMessage());
  //     }
  // }

  // DELETE /logistica/rutas/{id}
  public void eliminar(Context ctx) {
    UUID id = UUID.fromString(ctx.pathParam("id"));
    rutaRepo
        .buscarPorId(id)
        .ifPresentOrElse(
            ruta -> {
              rutaRepo.eliminar(id);
              ctx.status(204);
            },
            () -> ctx.status(404).result("Ruta no encontrada"));
  }

  // PATCH /logistica/rutas/{id}
  public void actualizar(Context ctx) {
    try {
      UUID id = UUID.fromString(ctx.pathParam("id"));

      rutaRepo
          .buscarPorId(id)
          .ifPresentOrElse(
              ruta -> {
                // Leemos la actualización desde el body
                RutaUpdateDTO dto = ctx.bodyAsClass(RutaUpdateDTO.class);

                if (dto.estado != null) {
                  try {
                    if ("EN_CURSO".equalsIgnoreCase(dto.estado)) {
                      ruta.iniciar();
                      // TODO: (Integración S2S - Issue #9) Avisar a Donaciones
                    } else if ("FINALIZADA".equalsIgnoreCase(dto.estado)) {
                      ruta.finalizar();
                    } else {
                      ctx.status(400)
                          .result("Estado inválido. Valores permitidos: EN_CURSO, FINALIZADA");
                      return;
                    }
                  } catch (IllegalStateException e) {
                    ctx.status(400).result(e.getMessage());
                    return;
                  }
                }

                ctx.status(200).json(ruta);
              },
              () -> ctx.status(404).result("Ruta no encontrada"));
    } catch (IllegalArgumentException e) {
      ctx.status(400).result("Formato de UUID inválido");
    }
  }

  // --- DTOs Internos ---
  public static class RutaDTO {
    public String patenteCamion;
    public List<Destino> destinos;
  }

  public static class RutaUpdateDTO {
    public String estado;
  }
}
