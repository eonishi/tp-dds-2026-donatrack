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

    // GET /api/logistica/rutas
    public void listar(Context ctx) {
        ctx.json(rutaRepo.listarTodas());
    }

    // GET /api/logistica/rutas/{id}
    public void buscar(Context ctx) {
        try {
            UUID id = UUID.fromString(ctx.pathParam("id"));
            Optional<Ruta> rutaOpt = rutaRepo.buscarPorId(id);

            if (rutaOpt.isPresent()) {
                ctx.json(rutaOpt.get());
            } else {
                ctx.status(404).result("Ruta no encontrada");
            }
        } catch (IllegalArgumentException e) {
            ctx.status(400).result("Formato de UUID inválido");
        }
    }

    // POST /api/logistica/rutas
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

    // PATCH /api/logistica/rutas/{id}/iniciar
    public void iniciar(Context ctx) {
        UUID id = UUID.fromString(ctx.pathParam("id"));
        Optional<Ruta> rutaOpt = rutaRepo.buscarPorId(id);

        if (rutaOpt.isEmpty()) {
            ctx.status(404).result("Ruta no encontrada");
            return;
        }

        Ruta ruta = rutaOpt.get();
        try {
            ruta.iniciar(); // Usa tu lógica de dominio

            // TODO: (Integración S2S - Issue #9) 
            // Hacer petición PATCH a /api/donaciones/estado para pasar 
            // las donaciones a EN_TRASLADO y enviar el idCamion.
            ctx.status(200).json(ruta);
        } catch (IllegalStateException e) {
            ctx.status(400).result(e.getMessage());
        }
    }

    // PATCH /api/logistica/rutas/{id}/finalizar
    public void finalizar(Context ctx) {
        UUID id = UUID.fromString(ctx.pathParam("id"));
        Optional<Ruta> rutaOpt = rutaRepo.buscarPorId(id);

        if (rutaOpt.isEmpty()) {
            ctx.status(404).result("Ruta no encontrada");
            return;
        }

        Ruta ruta = rutaOpt.get();
        try {
            ruta.finalizar(); // Usa tu lógica de dominio
            ctx.status(200).json(ruta);
        } catch (IllegalStateException e) {
            ctx.status(400).result(e.getMessage());
        }
    }

    // DELETE /api/logistica/rutas/{id}
    public void eliminar(Context ctx) {
        UUID id = UUID.fromString(ctx.pathParam("id"));
        if (rutaRepo.buscarPorId(id).isPresent()) {
            rutaRepo.eliminar(id);
            ctx.status(204);
        } else {
            ctx.status(404).result("Ruta no encontrada");
        }
    }

    // --- DTO Interno ---
    public static class RutaDTO {
        public String patenteCamion;
        public List<Destino> destinos;
    }
}