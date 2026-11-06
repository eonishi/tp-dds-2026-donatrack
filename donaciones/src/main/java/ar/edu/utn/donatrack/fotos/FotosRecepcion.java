package ar.edu.utn.donatrack.fotos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Valida y guarda las fotos con las que una entidad confirma la recepción de una donación.
 *
 * <p>El tipo se decide por los primeros bytes del archivo y no por la extensión o el tipo que
 * declara el navegador, que el usuario controla.
 */
public final class FotosRecepcion {
  public static final int MAXIMO_FOTOS = 5;
  public static final long TAMANIO_MAXIMO = 5L * 1024 * 1024;
  private static final Map<String, String> EXTENSIONES =
      Map.of("image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");

  private final AlmacenFotos almacen;

  public FotosRecepcion(AlmacenFotos almacen) {
    this.almacen = Objects.requireNonNull(almacen);
  }

  /** Una foto tal como llega del formulario. */
  public record FotoSubida(String nombreOriginal, byte[] contenido) {}

  /**
   * @return las URLs públicas de las fotos guardadas, en el orden recibido
   * @throws IllegalArgumentException si no hay fotos, son demasiadas, pesan de más o no son
   *     imágenes JPG, PNG o WebP
   */
  public List<String> guardar(UUID donacionId, List<FotoSubida> fotos) throws IOException {
    if (fotos.isEmpty()) {
      throw new IllegalArgumentException("Subí al menos una foto de la donación recibida.");
    }
    if (fotos.size() > MAXIMO_FOTOS) {
      throw new IllegalArgumentException("Podés subir hasta " + MAXIMO_FOTOS + " fotos.");
    }
    List<String> tipos = new ArrayList<>();
    for (FotoSubida foto : fotos) {
      if (foto.contenido().length > TAMANIO_MAXIMO) {
        throw new IllegalArgumentException(
            "La foto " + foto.nombreOriginal() + " pesa más de 5 MB.");
      }
      tipos.add(
          tipoDeImagen(foto.contenido())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "La foto " + foto.nombreOriginal() + " no es JPG, PNG ni WebP.")));
    }
    List<String> urls = new ArrayList<>();
    for (int i = 0; i < fotos.size(); i++) {
      String nombre = donacionId + "-" + UUID.randomUUID() + "." + EXTENSIONES.get(tipos.get(i));
      urls.add(almacen.guardar(nombre, tipos.get(i), fotos.get(i).contenido()));
    }
    return urls;
  }

  static Optional<String> tipoDeImagen(byte[] b) {
    if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
      return Optional.of("image/jpeg");
    }
    if (b.length >= 8
        && (b[0] & 0xFF) == 0x89
        && b[1] == 'P'
        && b[2] == 'N'
        && b[3] == 'G'
        && b[4] == 0x0D
        && b[5] == 0x0A
        && b[6] == 0x1A
        && b[7] == 0x0A) {
      return Optional.of("image/png");
    }
    if (b.length >= 12
        && b[0] == 'R'
        && b[1] == 'I'
        && b[2] == 'F'
        && b[3] == 'F'
        && b[8] == 'W'
        && b[9] == 'E'
        && b[10] == 'B'
        && b[11] == 'P') {
      return Optional.of("image/webp");
    }
    return Optional.empty();
  }
}
