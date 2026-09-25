package ar.edu.utn.donatrack.api.controller;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;

import ar.edu.utn.donatrack.api.error.ApiException;
import ar.edu.utn.donatrack.api.validation.DtoValidator;
import ar.edu.utn.donatrack.bienes.Categoria;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.bienes.SubcategoriasRepository;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.http.Context;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;

/**
 * Expone el catálogo de subcategorías: lo lista y permite dar de alta una entrada.
 *
 * <p>El alta es explícita: si el nombre ya está en el catálogo devuelve 409, igual que los demás
 * recursos del servicio con clave natural. Las cargas de donaciones y de necesidades no pasan por
 * acá para resolver su subcategoría: reutilizan la entrada existente a través del repositorio.
 */
public final class SubcategoriasController implements WithSimplePersistenceUnit {
  private final SubcategoriasRepository subcategorias;

  public SubcategoriasController(SubcategoriasRepository subcategorias) {
    this.subcategorias = subcategorias;
  }

  /** Cuerpo del alta de una entrada del catálogo. */
  public record SubcategoriaRequest(@NotBlank String nombre, @NotNull Categoria categoria) {}

  /** Representación HTTP de una entrada del catálogo. */
  public record SubcategoriaResponse(String nombre, Categoria categoria) {}

  public void routes() {
    path(
        "/subcategorias",
        () -> {
          get(this::listar);
          post(this::crear);
        });
  }

  private void listar(Context ctx) {
    ctx.json(
        subcategorias.getSubcategorias().stream().map(SubcategoriasController::response).toList());
  }

  private void crear(Context ctx) {
    SubcategoriaRequest request = DtoValidator.validate(ctx.bodyAsClass(SubcategoriaRequest.class));
    SubcategoriaResponse creada =
        withTransaction(
            () -> {
              if (subcategorias.buscarPorNombre(request.nombre()).isPresent()) {
                throw ApiException.conflict("Ya existe una subcategoría con ese nombre.");
              }
              return response(
                  subcategorias.obtenerOrCrear(
                      request.nombre(), request.categoria(), new HashMap<>()));
            });
    ctx.status(201).json(creada);
  }

  private static SubcategoriaResponse response(SubCategoria subcategoria) {
    return new SubcategoriaResponse(subcategoria.getNombre(), subcategoria.getCategoria());
  }
}
