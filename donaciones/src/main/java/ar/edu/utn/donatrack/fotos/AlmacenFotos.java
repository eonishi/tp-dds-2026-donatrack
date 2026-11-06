package ar.edu.utn.donatrack.fotos;

import java.io.IOException;

/**
 * Guarda las fotos que sube una entidad beneficiaria al confirmar la recepción de una donación y
 * devuelve la URL pública con la que se muestran.
 *
 * <p>En producción las fotos van a Supabase Storage (servidas por su CDN); sin credenciales se usa
 * un directorio local, pensado para desarrollo y pruebas.
 */
public interface AlmacenFotos {

  /**
   * @param nombre nombre único del archivo, ya generado por el sistema (nunca el del usuario)
   * @param tipoContenido tipo MIME de la imagen
   * @param contenido bytes de la imagen
   * @return la URL pública de la foto
   */
  String guardar(String nombre, String tipoContenido, byte[] contenido) throws IOException;
}
