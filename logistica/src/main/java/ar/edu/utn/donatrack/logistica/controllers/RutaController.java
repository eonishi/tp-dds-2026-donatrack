package ar.edu.utn.donatrack.logistica.controllers;

import ar.edu.utn.donatrack.logistica.Camion;
import ar.edu.utn.donatrack.logistica.CamionRepository;
import ar.edu.utn.donatrack.logistica.Destino;
import ar.edu.utn.donatrack.logistica.Ruta;
import ar.edu.utn.donatrack.logistica.RutaRepository;
import ar.edu.utn.donatrack.logistica.planificacion.DonacionesRemotas;
import ar.edu.utn.donatrack.logistica.security.LogisticaSecurity;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.http.Context;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RutaController implements WithSimplePersistenceUnit {

  private final RutaRepository rutaRepo;
  private final CamionRepository camionRepo;
  private final DonacionesRemotas donacionesRemotas;

  public RutaController(
      RutaRepository rutaRepo, CamionRepository camionRepo, DonacionesRemotas donacionesRemotas) {
    this.rutaRepo = rutaRepo;
    this.camionRepo = camionRepo;
    this.donacionesRemotas = donacionesRemotas;
  }

  // GET /logistica/rutas
  public void listar(Context ctx) {
    UUID repartidor = ctx.attribute(LogisticaSecurity.ATRIBUTO_REPARTIDOR);
    if (repartidor == null) {
      ctx.json(rutaRepo.listarTodas());
      return;
    }
    // El repartidor ve en su aplicación solo las rutas que tiene asignadas.
    ctx.json(rutaRepo.listarTodas().stream().filter(r -> r.tieneAsignado(repartidor)).toList());
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
    RutaDto dto = ctx.bodyAsClass(RutaDto.class);

    // 1. Buscamos el camión referenciado en el JSON
    Optional<Camion> camionOpt = camionRepo.buscarPorPatente(dto.patenteCamion);
    if (camionOpt.isEmpty()) {
      ctx.status(400).result("No se puede crear la ruta: Camión inexistente");
      return;
    }

    try {
      // 2. Instanciamos la ruta y la guardamos dentro de la transacción que abre este
      // controlador como dueño de la unidad de trabajo
      Ruta nuevaRuta =
          withTransaction(
              () -> {
                Ruta ruta = new Ruta(camionOpt.get(), dto.destinos);
                rutaRepo.agregar(ruta);
                return ruta;
              });

      // 3. Respondemos 201 Created
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
    try {
      UUID id = UUID.fromString(ctx.pathParam("id"));
      rutaRepo
          .buscarPorId(id)
          .ifPresentOrElse(
              ruta -> {
                withTransaction(() -> rutaRepo.eliminar(id));
                ctx.status(204);
              },
              () -> ctx.status(404).result("Ruta no encontrada"));
    } catch (IllegalArgumentException e) {
      ctx.status(400).result("Formato de UUID inválido");
    }
  }

  // PATCH /logistica/rutas/{id}
  public void actualizar(Context ctx) {
    try {
      UUID id = UUID.fromString(ctx.pathParam("id"));

      rutaRepo
          .buscarPorId(id)
          .ifPresentOrElse(
              ruta -> {
                UUID repartidor = ctx.attribute(LogisticaSecurity.ATRIBUTO_REPARTIDOR);
                if (repartidor != null && !ruta.tieneAsignado(repartidor)) {
                  ctx.status(403).result("El repartidor no está asignado a esta ruta");
                  return;
                }

                // Leemos la actualización desde el body
                RutaUpdateDto dto = ctx.bodyAsClass(RutaUpdateDto.class);

                if (dto.estado != null) {
                  try {
                    if ("EN_CURSO".equalsIgnoreCase(dto.estado)) {
                      // El cambio de estado y la lectura de los destinos van dentro de la
                      // transacción del controlador; la notificación sale después de guardarlo.
                      List<UUID> donaciones =
                          withTransaction(
                              () -> {
                                ruta.iniciar();
                                return ruta.getDestinos().stream()
                                    .flatMap(d -> d.donaciones().stream())
                                    .toList();
                              });
                      donacionesRemotas.notificarInicioTraslado(
                          donaciones, ruta.getCamion().getPatente());
                    } else if ("FINALIZADA".equalsIgnoreCase(dto.estado)) {
                      withTransaction(ruta::finalizar);
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
  public static class RutaDto {
    public String patenteCamion;
    public List<Destino> destinos;
  }

  public static class RutaUpdateDto {
    public String estado;
  }
}
