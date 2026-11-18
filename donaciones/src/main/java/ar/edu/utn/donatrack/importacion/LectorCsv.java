package ar.edu.utn.donatrack.importacion;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * Abre un CSV subido por una persona sin pedirle que sepa en qué codificación lo guardó.
 *
 * <p>Excel en Windows guarda "CSV" en Windows-1252 (Latin-1) y "CSV UTF-8" con BOM; otras
 * herramientas guardan UTF-8 sin BOM. Leer un archivo Latin-1 como UTF-8 rompe los acentos
 * ("Benjam�n"), incluso dentro de los correos. Por eso se mira el principio del archivo: si no es
 * UTF-8 válido, se lee como Windows-1252. El archivo se sigue leyendo de a partes, nunca entero.
 */
public final class LectorCsv {
  static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
  // Alcanza para el encabezado ("Razón Social", "Teléfono") y cientos de filas.
  private static final int MUESTRA = 64 * 1024;

  private LectorCsv() {}

  public static Reader abrir(InputStream contenido) throws IOException {
    BufferedInputStream entrada = new BufferedInputStream(contenido, MUESTRA);
    entrada.mark(MUESTRA);
    byte[] muestra = entrada.readNBytes(MUESTRA);
    entrada.reset();
    if (tieneBomUtf8(muestra)) {
      entrada.skipNBytes(3);
      return lector(entrada, StandardCharsets.UTF_8);
    }
    return lector(entrada, esUtf8(muestra) ? StandardCharsets.UTF_8 : WINDOWS_1252);
  }

  private static Reader lector(InputStream entrada, Charset codificacion) {
    return new BufferedReader(new InputStreamReader(entrada, codificacion));
  }

  private static boolean tieneBomUtf8(byte[] b) {
    return b.length >= 3 && (b[0] & 0xFF) == 0xEF && (b[1] & 0xFF) == 0xBB && (b[2] & 0xFF) == 0xBF;
  }

  /**
   * Si la muestra decodifica como UTF-8 sin errores. Un carácter de varios bytes cortado justo al
   * final de la muestra no cuenta como error: el resto llegaría en los bytes siguientes.
   */
  static boolean esUtf8(byte[] muestra) {
    CharsetDecoder decodificador =
        StandardCharsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);
    CharBuffer destino = CharBuffer.allocate(muestra.length);
    CoderResult resultado = decodificador.decode(ByteBuffer.wrap(muestra), destino, false);
    return !resultado.isError();
  }
}
