package ar.edu.utn.donatrack.fotos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Guarda las fotos en un directorio del servidor y las publica en {@code /fotos/{nombre}}.
 *
 * <p>Es para desarrollo y pruebas: en un despliegue el disco del contenedor no sobrevive a un
 * redeploy, por eso producción usa {@link AlmacenFotosSupabase}.
 */
public final class AlmacenFotosLocal implements AlmacenFotos {
  public static final String RUTA_PUBLICA = "/fotos/";
  private static final Pattern NOMBRE_VALIDO = Pattern.compile("[a-z0-9-]+\\.(jpg|png|webp)");

  private final Path directorio;

  public AlmacenFotosLocal(Path directorio) {
    this.directorio = Objects.requireNonNull(directorio);
  }

  @Override
  public String guardar(String nombre, String tipoContenido, byte[] contenido) throws IOException {
    Files.createDirectories(directorio);
    Files.write(archivo(nombre).orElseThrow(), contenido);
    return RUTA_PUBLICA + nombre;
  }

  /**
   * Resuelve una foto guardada. Solo acepta nombres generados por el sistema, así que un pedido
   * como {@code ../../etc/passwd} nunca sale del directorio.
   */
  public Optional<Path> archivo(String nombre) {
    if (nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches()) {
      return Optional.empty();
    }
    return Optional.of(directorio.resolve(nombre));
  }
}
