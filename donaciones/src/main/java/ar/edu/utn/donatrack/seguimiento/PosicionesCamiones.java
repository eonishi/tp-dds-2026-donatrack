package ar.edu.utn.donatrack.seguimiento;

import java.util.Optional;

/** Consulta a logística por la posición de los camiones. */
public interface PosicionesCamiones {

  /**
   * @param patente patente del camión
   * @return la última posición, o vacío si el camión todavía no reportó ninguna
   */
  Optional<PosicionCamion> ultimaPosicion(String patente);
}
