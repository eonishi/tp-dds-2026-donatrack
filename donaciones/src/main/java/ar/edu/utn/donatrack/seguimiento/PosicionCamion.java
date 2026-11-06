package ar.edu.utn.donatrack.seguimiento;

/**
 * Última posición conocida de un camión, tal como la informa logística.
 *
 * @param patente patente del camión
 * @param latitud latitud en grados decimales
 * @param longitud longitud en grados decimales
 * @param actualizadaEn fecha y hora de la lectura, en el formato ISO que devuelve logística
 */
public record PosicionCamion(
    String patente, double latitud, double longitud, String actualizadaEn) {}
