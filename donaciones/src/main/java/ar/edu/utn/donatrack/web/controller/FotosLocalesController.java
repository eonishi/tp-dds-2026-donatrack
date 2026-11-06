package ar.edu.utn.donatrack.web.controller;

import static io.javalin.apibuilder.ApiBuilder.get;

import ar.edu.utn.donatrack.fotos.AlmacenFotosLocal;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Sirve las fotos del almacén local; con Supabase las sirve su CDN y esta ruta no se registra. */
public class FotosLocalesController {
  private final AlmacenFotosLocal almacen;

  public FotosLocalesController(AlmacenFotosLocal almacen) {
    this.almacen = almacen;
  }

  public void routes() {
    get(AlmacenFotosLocal.RUTA_PUBLICA + "{nombre}", this::servir);
  }

  private void servir(Context ctx) throws IOException {
    Path archivo =
        almacen
            .archivo(ctx.pathParam("nombre"))
            .filter(Files::isRegularFile)
            .orElseThrow(NotFoundResponse::new);
    String nombre = archivo.getFileName().toString();
    String tipo =
        nombre.endsWith(".png")
            ? "image/png"
            : nombre.endsWith(".webp") ? "image/webp" : "image/jpeg";
    ctx.contentType(tipo)
        .header("X-Content-Type-Options", "nosniff")
        .header("Cache-Control", "public, max-age=31536000, immutable")
        .result(Files.readAllBytes(archivo));
  }
}
