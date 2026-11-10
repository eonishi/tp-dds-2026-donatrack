package ar.edu.utn.donatrack.fotos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.donatrack.fotos.FotosRecepcion.FotoSubida;
import io.javalin.Javalin;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FotosRecepcionTest {
  private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};
  private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
  private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};

  @TempDir Path directorio;

  @Test
  void reconoceLasImagenesPorSuContenidoYNoPorElNombre() throws Exception {
    FotosRecepcion fotos = new FotosRecepcion(new AlmacenFotosLocal(directorio));
    UUID donacion = UUID.randomUUID();

    List<String> urls =
        fotos.guardar(
            donacion,
            List.of(
                new FotoSubida("a.png", JPG),
                new FotoSubida("b.exe", PNG),
                new FotoSubida("c", WEBP)));

    assertTrue(urls.get(0).matches("/fotos/" + donacion + "-[a-z0-9-]+\\.jpg"));
    assertTrue(urls.get(1).endsWith(".png"));
    assertTrue(urls.get(2).endsWith(".webp"));
    assertEquals(3, archivosGuardados());
  }

  @Test
  void rechazaArchivosQueNoSonImagenesSinGuardarNinguno() throws Exception {
    FotosRecepcion fotos = new FotosRecepcion(new AlmacenFotosLocal(directorio));
    byte[] svg = "<svg/>".getBytes(StandardCharsets.UTF_8);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            fotos.guardar(
                UUID.randomUUID(),
                List.of(new FotoSubida("ok.png", PNG), new FotoSubida("x.png", svg))));
    assertEquals(0, archivosGuardados());
  }

  @Test
  void exigeEntreUnaYCincoFotos() {
    FotosRecepcion fotos = new FotosRecepcion(new AlmacenFotosLocal(directorio));
    List<FotoSubida> seis = new ArrayList<>(Collections.nCopies(6, new FotoSubida("a.png", PNG)));

    assertThrows(IllegalArgumentException.class, () -> fotos.guardar(UUID.randomUUID(), List.of()));
    assertThrows(IllegalArgumentException.class, () -> fotos.guardar(UUID.randomUUID(), seis));
  }

  @Test
  void supabaseRecibeLaFotoConLaClaveYDevuelveLaUrlPublica() throws Exception {
    List<String> recibido = new ArrayList<>();
    Javalin storage =
        Javalin.create()
            .post(
                "/storage/v1/object/recepciones/{nombre}",
                ctx -> {
                  recibido.add(ctx.header("Authorization"));
                  recibido.add(ctx.contentType());
                  recibido.add(String.valueOf(ctx.bodyAsBytes().length));
                  ctx.json(Map.of("Key", "recepciones/" + ctx.pathParam("nombre")));
                })
            .start(0);
    try {
      String base = "http://localhost:" + storage.port();
      AlmacenFotos almacen =
          new AlmacenFotosSupabase(HttpClient.newHttpClient(), base + "/", "clave", "recepciones");

      String publica = almacen.guardar("foto.png", "image/png", PNG);

      assertEquals(base + "/storage/v1/object/public/recepciones/foto.png", publica);
      assertEquals(List.of("Bearer clave", "image/png", String.valueOf(PNG.length)), recibido);
    } finally {
      storage.stop();
    }
  }

  @Test
  void siSupabaseRechazaLaFotoFallaConUnError() {
    Javalin storage =
        Javalin.create().post("/storage/v1/object/b/{nombre}", ctx -> ctx.status(403)).start(0);
    try {
      AlmacenFotos almacen =
          new AlmacenFotosSupabase(
              HttpClient.newHttpClient(), "http://localhost:" + storage.port(), "mala", "b");

      assertThrows(IOException.class, () -> almacen.guardar("f.png", "image/png", PNG));
    } finally {
      storage.stop();
    }
  }

  @Test
  void eligeSupabaseSoloConSusCredenciales() {
    assertInstanceOf(
        AlmacenFotosSupabase.class,
        AlmacenFotosDesdeEntorno.crear(
            Map.of("SUPABASE_URL", "https://x.supabase.co", "SUPABASE_SERVICE_KEY", "k")));
    assertInstanceOf(
        AlmacenFotosLocal.class,
        AlmacenFotosDesdeEntorno.crear(Map.of("FOTOS_DIR", directorio.toString())));
  }

  private long archivosGuardados() throws IOException {
    if (Files.notExists(directorio)) {
      return 0;
    }
    try (Stream<Path> archivos = Files.list(directorio)) {
      return archivos.count();
    }
  }
}
