package ar.edu.utn.donatrack.api;

import ar.edu.utn.donatrack.flota.FlotaLogistica;
import ar.edu.utn.donatrack.flota.FlotaLogisticaGraphql;
import ar.edu.utn.donatrack.seguimiento.PosicionesCamiones;
import ar.edu.utn.donatrack.seguimiento.PosicionesCamionesGraphql;
import ar.edu.utn.donatrack.ubicacion.Geocodificador;
import ar.edu.utn.donatrack.ubicacion.GeocodificadorNominatim;

/**
 * Lo que donaciones consulta afuera: logística (posiciones y flota) y la geocodificación. Las
 * pruebas lo reemplazan por dobles para no depender de servicios externos.
 */
public record ServiciosExternos(
    PosicionesCamiones posiciones, FlotaLogistica flota, Geocodificador geocodificador) {

  public static ServiciosExternos desdeEntorno() {
    return new ServiciosExternos(
        PosicionesCamionesGraphql.desdeEntorno(),
        FlotaLogisticaGraphql.desdeEntorno(),
        GeocodificadorNominatim.desdeEntorno());
  }

  public ServiciosExternos conPosiciones(PosicionesCamiones otras) {
    return new ServiciosExternos(otras, flota, geocodificador);
  }
}
