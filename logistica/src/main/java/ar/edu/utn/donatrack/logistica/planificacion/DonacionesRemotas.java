package ar.edu.utn.donatrack.logistica.planificacion;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de logística hacia el microservicio de donaciones.
 *
 * <p>Logística no puede depender de los repositorios de donaciones (viven en el otro servicio), así
 * que declara acá lo único que necesita de él: las donaciones listas para planificar y el aviso de
 * cambio de estado cuando las rutas quedan armadas. El adaptador REST de la integración implementa
 * este puerto con los endpoints del contrato.
 */
public interface DonacionesRemotas {

  /**
   * Lista las donaciones listas para armar rutas.
   *
   * @return las donaciones en estado {@code ASIGNACION_REALIZADA}, con los datos de planificación
   */
  List<DonacionPlanificable> listarEnAsignacionRealizada();

  /**
   * Notifica que las donaciones indicadas pasaron a {@code LISTA_PARA_ENTREGAR}.
   *
   * @param donaciones identificadores de las donaciones planificadas (no nulo)
   */
  void notificarListaParaEntregar(List<UUID> donaciones);

  void notificarInicioTraslado(List<UUID> donaciones, String idCamion);
}
