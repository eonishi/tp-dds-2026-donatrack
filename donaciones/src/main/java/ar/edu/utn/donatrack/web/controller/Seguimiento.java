package ar.edu.utn.donatrack.web.controller;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.EstadoDonacion;
import ar.edu.utn.donatrack.seguimiento.LogisticaNoDisponibleException;
import ar.edu.utn.donatrack.seguimiento.PosicionCamion;
import ar.edu.utn.donatrack.seguimiento.PosicionesCamiones;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Arma lo que muestran los mapas de seguimiento: cada donación en camino con su camión (última
 * posición y hora) y el destino. Un camión que lleva varias donaciones se consulta una sola vez.
 */
final class Seguimiento {
  private final PosicionesCamiones posiciones;

  Seguimiento(PosicionesCamiones posiciones) {
    this.posiciones = posiciones;
  }

  /** Lo que recibe el mapa por JSON. Solo datos para mostrar: nada de la persona donante. */
  record Entrega(
      String donacion,
      List<String> bienes,
      String entidad,
      Double destinoLatitud,
      Double destinoLongitud,
      String camion,
      Double latitud,
      Double longitud,
      String actualizadaEn,
      String aviso) {}

  List<Entrega> entregas(List<Donacion> donaciones) {
    Map<String, Optional<PosicionCamion>> porCamion = new HashMap<>();
    Map<String, String> fallas = new LinkedHashMap<>();
    return donaciones.stream()
        .filter(d -> d.getEstado() == EstadoDonacion.EN_TRASLADO)
        .map(
            d -> {
              String patente = d.getIdCamion().orElse(null);
              Optional<PosicionCamion> posicion = Optional.empty();
              String aviso = null;
              if (patente == null) {
                aviso = "Todavía no tiene un camión asignado.";
              } else {
                if (!porCamion.containsKey(patente) && !fallas.containsKey(patente)) {
                  try {
                    porCamion.put(patente, posiciones.ultimaPosicion(patente));
                  } catch (LogisticaNoDisponibleException e) {
                    fallas.put(patente, "No pudimos consultar la ubicación del camión.");
                  }
                }
                aviso = fallas.get(patente);
                posicion = porCamion.getOrDefault(patente, Optional.empty());
                if (aviso == null && posicion.isEmpty()) {
                  aviso = "El camión todavía no informó su ubicación.";
                }
              }
              EntidadBeneficiaria entidad = d.getEntidadBeneficiaria();
              return new Entrega(
                  d.getId().toString(),
                  Vistas.bienes(d),
                  entidad == null ? "" : entidad.getRazonSocial(),
                  entidad == null ? null : entidad.getLatitud().orElse(null),
                  entidad == null ? null : entidad.getLongitud().orElse(null),
                  patente,
                  posicion.map(PosicionCamion::latitud).orElse(null),
                  posicion.map(PosicionCamion::longitud).orElse(null),
                  posicion.map(PosicionCamion::actualizadaEn).map(Seguimiento::hora).orElse(null),
                  aviso);
            })
        .toList();
  }

  /** "2026-09-11T11:59:00" → "11/09/2026 11:59". */
  private static String hora(String iso) {
    if (iso == null || iso.length() < 16) {
      return iso;
    }
    return iso.substring(8, 10)
        + "/"
        + iso.substring(5, 7)
        + "/"
        + iso.substring(0, 4)
        + " "
        + iso.substring(11, 16);
  }
}
