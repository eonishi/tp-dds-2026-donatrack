package ar.edu.utn.donatrack.ubicacion;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Completa la ubicación de las entidades a partir de su dirección.
 *
 * <p>Corre en segundo plano y no en el alta, para que registrarse no dependa de un servicio externo
 * y para respetar el límite de Nominatim (un pedido por segundo): una entidad nueva aparece en los
 * mapas en la pasada siguiente.
 */
public class UbicadorEntidades implements WithSimplePersistenceUnit {
  private static final Logger LOGGER = LoggerFactory.getLogger(UbicadorEntidades.class);
  private static final int POR_PASADA = 30;

  private final Geocodificador geocodificador;
  private final long pausaMillis;

  public UbicadorEntidades(Geocodificador geocodificador, long pausaMillis) {
    this.geocodificador = geocodificador;
    this.pausaMillis = pausaMillis;
  }

  /**
   * Ubica hasta {@value #POR_PASADA} entidades pendientes.
   *
   * @return cuántas quedaron ubicadas en esta pasada
   */
  public int ubicarPendientes() {
    entityManager().clear();
    List<Long> pendientes =
        createQuery(
                "select e.id from EntidadBeneficiaria e where e.ubicacionIntentada = false"
                    + " and e.direccion is not null",
                Long.class)
            .setMaxResults(POR_PASADA)
            .getResultList();
    int ubicadas = 0;
    for (Long id : pendientes) {
      EntidadBeneficiaria entidad = find(EntidadBeneficiaria.class, id);
      if (entidad == null || !entidad.faltaUbicar()) {
        continue;
      }
      Optional<Coordenadas> coordenadas;
      try {
        coordenadas = geocodificador.ubicar(entidad.getDireccion());
      } catch (GeocodificacionNoDisponibleException e) {
        LOGGER.warn("Geocodificación no disponible; se reintenta en la próxima", e);
        return ubicadas;
      }
      withTransaction(
          () ->
              coordenadas.ifPresentOrElse(
                  c -> entidad.ubicar(c.latitud(), c.longitud()), entidad::marcarSinUbicacion));
      if (coordenadas.isPresent()) {
        ubicadas++;
      }
      pausar();
    }
    return ubicadas;
  }

  private void pausar() {
    if (pausaMillis <= 0) {
      return;
    }
    try {
      Thread.sleep(pausaMillis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
