package ar.edu.utn.donatrack.importacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class LectorCsvTest {
  private static final String TEXTO = "Nombre/Razón Social,Teléfono\nBenjamín Castro,ñandú\n";

  @Test
  void leeUtf8SinBom() throws IOException {
    assertEquals(TEXTO, leer(TEXTO.getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void leeUtf8ConBomSinDejarloEnElTexto() throws IOException {
    byte[] cuerpo = TEXTO.getBytes(StandardCharsets.UTF_8);
    byte[] conBom = new byte[cuerpo.length + 3];
    conBom[0] = (byte) 0xEF;
    conBom[1] = (byte) 0xBB;
    conBom[2] = (byte) 0xBF;
    System.arraycopy(cuerpo, 0, conBom, 3, cuerpo.length);
    assertEquals(TEXTO, leer(conBom));
  }

  @Test
  void leeLatin1ComoLoGuardaExcelEnWindows() throws IOException {
    assertEquals(TEXTO, leer(TEXTO.getBytes(LectorCsv.WINDOWS_1252)));
  }

  @Test
  void unCaracterUtf8CortadoAlFinalDeLaMuestraNoLaHaceLatin1() {
    byte[] utf8 = "abc é".getBytes(StandardCharsets.UTF_8);
    byte[] cortado = Arrays.copyOf(utf8, utf8.length - 1); // queda solo el primer byte de la é
    assertTrue(LectorCsv.esUtf8(cortado));
  }

  private static String leer(byte[] bytes) throws IOException {
    try (Reader lector = LectorCsv.abrir(new ByteArrayInputStream(bytes))) {
      StringWriter texto = new StringWriter();
      lector.transferTo(texto);
      return texto.toString();
    }
  }
}
