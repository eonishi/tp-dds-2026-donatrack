package ar.edu.utn.donatrack.logistica.controllers;

import ar.edu.utn.donatrack.logistica.Camion;
import ar.edu.utn.donatrack.logistica.CamionRepository;
import io.javalin.http.Context;

public class CamionController {

  private final CamionRepository repositorio;

  // Inyectamos el repositorio por constructor
  public CamionController(CamionRepository repositorio) {
    this.repositorio = repositorio;
  }

  // GET /logistica/camiones
  public void listar(Context ctx) {
    ctx.json(repositorio.listarTodos());
  }

  // GET /logistica/camiones/{patente}
  public void buscar(Context ctx) {
    String patente = ctx.pathParam("patente");

    repositorio
        .buscarPorPatente(patente)
        .ifPresentOrElse(ctx::json, () -> ctx.status(404).result("Camión no encontrado"));
  }

  // POST /logistica/camiones
  public void crear(Context ctx) {
    try {
      // 1. Mapeamos el JSON entrante a un DTO simple
      CamionDTO dto = ctx.bodyAsClass(CamionDTO.class);

      // 2. Usamos tu constructor que ya tiene las validaciones
      Camion nuevoCamion =
          new Camion(dto.patente, dto.volumenM3, dto.alturaM, dto.capacidadCargaKg);

      // 3. Guardamos y respondemos 201 Created
      repositorio.agregar(nuevoCamion);
      ctx.status(201).json(nuevoCamion);

    } catch (IllegalArgumentException e) {
      // Si el constructor del Camion tira error por datos inválidos (ej. volumen negativo)
      ctx.status(400).result(e.getMessage());
    }
  }

  // PUT /logistica/camiones/{patente}
  public void actualizar(Context ctx) {
    String patente = ctx.pathParam("patente");

    if (repositorio.buscarPorPatente(patente).isEmpty()) {
      ctx.status(404).result("Camión no encontrado");
      return;
    }

    try {
      CamionDTO dto = ctx.bodyAsClass(CamionDTO.class);

      // Nos aseguramos de que la patente del body coincida con la de la URL
      if (!patente.equals(dto.patente)) {
        ctx.status(400).result("La patente de la URL no coincide con la del cuerpo de la petición");
        return;
      }

      Camion camionActualizado =
          new Camion(dto.patente, dto.volumenM3, dto.alturaM, dto.capacidadCargaKg);

      // Tu método agregar funciona como "upsert", así que pisa el anterior gracias al
      // equals/hashCode
      repositorio.agregar(camionActualizado);
      ctx.status(200).json(camionActualizado);

    } catch (IllegalArgumentException e) {
      ctx.status(400).result(e.getMessage());
    }
  }

  // DELETE /logistica/camiones/{patente}
  public void eliminar(Context ctx) {
    String patente = ctx.pathParam("patente");

    repositorio
        .buscarPorPatente(patente)
        .ifPresentOrElse(
            camion -> {
              repositorio.eliminar(patente);
              ctx.status(204); // 204 No Content
            },
            () -> ctx.status(404).result("Camión no encontrado"));
  }

  // --- DTO Interno para recibir el JSON ---
  public static class CamionDTO {
    public String patente;
    public double volumenM3;
    public double alturaM;
    public double capacidadCargaKg;
  }
}
