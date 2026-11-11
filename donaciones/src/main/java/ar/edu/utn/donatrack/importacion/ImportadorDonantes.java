package ar.edu.utn.donatrack.importacion;

import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.security.AuthService;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Importa personas donantes desde un CSV (más de 10.000 filas) y les da una cuenta para activar.
 *
 * <p>Corre en segundo plano: la vista muestra el progreso. Lee el archivo fila por fila y guarda en
 * lotes de {@value #LOTE} por transacción, así que no carga el archivo entero en memoria ni deja
 * una transacción gigante abierta. Las cuentas se crean sin contraseña (pendientes de activación):
 * no hay que hashear 10.000 contraseñas ni mandarlas por correo.
 */
public class ImportadorDonantes implements WithSimplePersistenceUnit {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImportadorDonantes.class);
  static final int LOTE = 500;

  private final AuthService authService;
  private final Clock clock;
  private final ExecutorService ejecutor;
  private final Map<UUID, Importacion> importaciones = new ConcurrentHashMap<>();

  public ImportadorDonantes(AuthService authService, Clock clock) {
    this(authService, clock, Executors.newSingleThreadExecutor());
  }

  ImportadorDonantes(AuthService authService, Clock clock, ExecutorService ejecutor) {
    this.authService = authService;
    this.clock = clock;
    this.ejecutor = ejecutor;
  }

  /** Arranca la importación de un archivo ya guardado en disco; el archivo se borra al terminar. */
  public Importacion iniciar(String nombre, Path archivo) {
    Importacion importacion = new Importacion(nombre, LocalDateTime.now(clock));
    importaciones.put(importacion.getId(), importacion);
    ejecutor.submit(
        () -> {
          try (InputStream contenido = Files.newInputStream(archivo)) {
            importar(contenido, importacion);
          } catch (IOException | RuntimeException e) {
            LOGGER.warn("Falló la importación {}", importacion.getId(), e);
            importacion.fallar("No se pudo leer el archivo: revisá que sea un CSV válido.");
          } finally {
            try {
              Files.deleteIfExists(archivo);
            } catch (IOException e) {
              LOGGER.debug("No se pudo borrar el archivo temporal", e);
            }
          }
        });
    return importacion;
  }

  public Optional<Importacion> buscar(UUID id) {
    return Optional.ofNullable(importaciones.get(id));
  }

  public List<Importacion> recientes() {
    return importaciones.values().stream()
        .sorted((a, b) -> b.getInicio().compareTo(a.getInicio()))
        .limit(10)
        .toList();
  }

  public void detener() {
    ejecutor.shutdownNow();
  }

  /** Importa todo el contenido en el hilo actual (lo usa {@link #iniciar} y las pruebas). */
  void importar(InputStream contenido, Importacion importacion) throws IOException {
    entityManager().clear();
    Map<String, UUID> porCorreo = donantesPorCorreo();
    Map<String, UUID> porDocumento = donantesPorDocumento();
    Set<String> correosConCuenta = correosConCuenta();

    try (Reader lector = sinBom(contenido)) {
      CsvToBean<DonanteCsv> filas =
          new CsvToBeanBuilder<DonanteCsv>(lector)
              .withType(DonanteCsv.class)
              .withIgnoreLeadingWhiteSpace(true)
              .withThrowExceptions(false)
              .build();
      Iterator<DonanteCsv> iterador = filas.iterator();
      long numero = 1; // la fila 1 es el encabezado
      List<Fila> lote = new ArrayList<>(LOTE);
      while (iterador.hasNext()) {
        numero++;
        lote.add(new Fila(numero, iterador.next()));
        if (lote.size() == LOTE) {
          guardar(lote, importacion, porCorreo, porDocumento, correosConCuenta);
          lote.clear();
        }
      }
      guardar(lote, importacion, porCorreo, porDocumento, correosConCuenta);
      filas
          .getCapturedExceptions()
          .forEach(e -> importacion.error(e.getLineNumber(), "formato inválido"));
    }
    importacion.terminar();
  }

  private record Fila(long numero, DonanteCsv datos) {}

  private void guardar(
      List<Fila> lote,
      Importacion importacion,
      Map<String, UUID> porCorreo,
      Map<String, UUID> porDocumento,
      Set<String> correosConCuenta) {
    if (lote.isEmpty()) {
      return;
    }
    withTransaction(
        () -> {
          for (Fila fila : lote) {
            importacion.procesada();
            try {
              importarFila(fila.datos(), importacion, porCorreo, porDocumento, correosConCuenta);
            } catch (RuntimeException e) {
              importacion.error(fila.numero(), motivo(e));
            }
          }
        });
    // Cada lote se suelta del contexto de persistencia: si no, 10.000 donantes quedarían en
    // memoria y cada flush revisaría todos.
    entityManager().clear();
  }

  private void importarFila(
      DonanteCsv csv,
      Importacion importacion,
      Map<String, UUID> porCorreo,
      Map<String, UUID> porDocumento,
      Set<String> correosConCuenta) {
    String correo = obligatorio(csv.getEmail(), "falta el correo").trim().toLowerCase();
    if (!correo.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
      throw new IllegalArgumentException("el correo no es válido");
    }
    String documento =
        csv.getTipoDoc() + ":" + obligatorio(csv.getDocumento(), "falta el documento").trim();
    UUID donanteId = porCorreo.getOrDefault(correo, porDocumento.get(documento));
    if (donanteId == null) {
      Donante donante = ImportadorDonantesCsv.mapearDonante(csv);
      persist(donante);
      donanteId = donante.getId();
      porCorreo.put(correo, donanteId);
      porDocumento.put(documento, donanteId);
      importacion.donanteNuevo();
    } else {
      importacion.donanteExistente();
    }
    if (!correosConCuenta.contains(correo) && authService.agregarPendiente(correo, donanteId)) {
      importacion.cuentaCreada();
    }
    correosConCuenta.add(correo);
  }

  private static String obligatorio(String valor, String motivo) {
    if (valor == null || valor.isBlank()) {
      throw new IllegalArgumentException(motivo);
    }
    return valor;
  }

  private static String motivo(RuntimeException e) {
    if (e.getMessage() != null && e.getMessage().startsWith("No enum constant")) {
      return "tipo de persona o de documento inválido";
    }
    if (e instanceof IllegalArgumentException && e.getMessage() != null) {
      return e.getMessage();
    }
    return "datos inválidos";
  }

  private Map<String, UUID> donantesPorCorreo() {
    Map<String, UUID> mapa = new HashMap<>();
    for (Object[] fila :
        createQuery(
                "select lower(c.contacto), d.id from Donante d join d.mediosDeContacto c"
                    + " where type(c) = CorreoElectronico",
                Object[].class)
            .getResultList()) {
      mapa.putIfAbsent((String) fila[0], (UUID) fila[1]);
    }
    return mapa;
  }

  private Map<String, UUID> donantesPorDocumento() {
    Map<String, UUID> mapa = new HashMap<>();
    for (Object[] fila :
        createQuery("select d.documento, d.numDocumento, d.id from Donante d", Object[].class)
            .getResultList()) {
      mapa.putIfAbsent(fila[0] + ":" + fila[1], (UUID) fila[2]);
    }
    return mapa;
  }

  private Set<String> correosConCuenta() {
    return new HashSet<>(
        createQuery("select u.email from Usuario u", String.class).getResultList());
  }

  /** Saltea el BOM de UTF-8 que agrega Excel al guardar como CSV. */
  private static Reader sinBom(InputStream contenido) throws IOException {
    BufferedInputStream entrada = new BufferedInputStream(contenido);
    entrada.mark(3);
    byte[] inicio = entrada.readNBytes(3);
    if (inicio.length < 3
        || (inicio[0] & 0xFF) != 0xEF
        || (inicio[1] & 0xFF) != 0xBB
        || (inicio[2] & 0xFF) != 0xBF) {
      entrada.reset();
    }
    return new InputStreamReader(entrada, StandardCharsets.UTF_8);
  }
}
