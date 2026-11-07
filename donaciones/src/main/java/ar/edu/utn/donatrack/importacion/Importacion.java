package ar.edu.utn.donatrack.importacion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Progreso y resultado de una importación de donantes, para mostrarlo en la vista. */
public final class Importacion {
  private static final int MAXIMO_ERRORES = 50;

  private final UUID id = UUID.randomUUID();
  private final String archivo;
  private final LocalDateTime inicio;
  private volatile int procesadas;
  private volatile int donantesNuevos;
  private volatile int donantesExistentes;
  private volatile int cuentasCreadas;
  private volatile int filasConError;
  private volatile boolean terminada;
  private volatile String falla;
  private final List<String> errores = Collections.synchronizedList(new ArrayList<>());

  Importacion(String archivo, LocalDateTime inicio) {
    this.archivo = archivo;
    this.inicio = inicio;
  }

  void procesada() {
    procesadas++;
  }

  void donanteNuevo() {
    donantesNuevos++;
  }

  void donanteExistente() {
    donantesExistentes++;
  }

  void cuentaCreada() {
    cuentasCreadas++;
  }

  void error(long fila, String motivo) {
    filasConError++;
    if (errores.size() < MAXIMO_ERRORES) {
      errores.add("Fila " + fila + ": " + motivo);
    }
  }

  void terminar() {
    terminada = true;
  }

  void fallar(String motivo) {
    falla = motivo;
    terminada = true;
  }

  public UUID getId() {
    return id;
  }

  public String getArchivo() {
    return archivo;
  }

  public LocalDateTime getInicio() {
    return inicio;
  }

  public int getProcesadas() {
    return procesadas;
  }

  public int getDonantesNuevos() {
    return donantesNuevos;
  }

  public int getDonantesExistentes() {
    return donantesExistentes;
  }

  public int getCuentasCreadas() {
    return cuentasCreadas;
  }

  public int getFilasConError() {
    return filasConError;
  }

  public boolean isTerminada() {
    return terminada;
  }

  public boolean isEnCurso() {
    return !terminada;
  }

  public String getFalla() {
    return falla;
  }

  public List<String> getErrores() {
    return List.copyOf(errores);
  }
}
