package ar.edu.utn.donatrack.fotos;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

/** Elige el almacén de fotos según las variables de entorno. */
public final class AlmacenFotosDesdeEntorno {
  private static final Logger LOG = Logger.getLogger(AlmacenFotosDesdeEntorno.class.getName());
  private static final String BUCKET_POR_DEFECTO = "recepciones";

  private AlmacenFotosDesdeEntorno() {}

  public static AlmacenFotos crear() {
    return crear(System.getenv());
  }

  /**
   * Con {@code SUPABASE_URL} y {@code SUPABASE_SERVICE_KEY} usa Supabase Storage (bucket {@code
   * SUPABASE_BUCKET}, por defecto {@value #BUCKET_POR_DEFECTO}). Sin ellas guarda en {@code
   * FOTOS_DIR}, o en un directorio temporal, y lo avisa en el log.
   */
  static AlmacenFotos crear(Map<String, String> entorno) {
    String url = entorno.get("SUPABASE_URL");
    String clave = entorno.get("SUPABASE_SERVICE_KEY");
    if (presente(url) && presente(clave)) {
      String bucket = entorno.get("SUPABASE_BUCKET");
      return new AlmacenFotosSupabase(
          HttpClient.newHttpClient(), url, clave, presente(bucket) ? bucket : BUCKET_POR_DEFECTO);
    }
    String directorio = entorno.get("FOTOS_DIR");
    Path destino =
        presente(directorio)
            ? Path.of(directorio)
            : Path.of(System.getProperty("java.io.tmpdir"), "donatrack-fotos");
    LOG.warning(
        "Sin SUPABASE_URL/SUPABASE_SERVICE_KEY: las fotos se guardan en "
            + destino.toAbsolutePath());
    return new AlmacenFotosLocal(destino);
  }

  private static boolean presente(String valor) {
    return valor != null && !valor.isBlank();
  }
}
