package ar.edu.utn.donatrack.donaciones;

import ar.edu.utn.donatrack.estados.AsignacionRealizada;
import ar.edu.utn.donatrack.estados.EnDeposito;
import ar.edu.utn.donatrack.estados.EnTraslado;
import ar.edu.utn.donatrack.estados.EntregaFallida;
import ar.edu.utn.donatrack.estados.Entregada;
import ar.edu.utn.donatrack.estados.EstadoDonacion;
import ar.edu.utn.donatrack.estados.ListaParaEntregar;
import ar.edu.utn.donatrack.estados.Vencida;

public enum NombreEstado {
  EN_DEPOSITO,
  ASIGNACION_REALIZADA,
  LISTA_PARA_ENTREGAR,
  EN_TRASLADO,
  ENTREGADA,
  ENTREGA_FALLIDA,
  VENCIDA;

  /**
   * Builds the behavior object that matches this name.
   *
   * <p>States are not persisted: only their name is stored in the historial, so the state object
   * has to be rebuilt from the name when a donation is loaded from the database.
   */
  public EstadoDonacion crearEstado() {
    return switch (this) {
      case EN_DEPOSITO -> new EnDeposito();
      case ASIGNACION_REALIZADA -> new AsignacionRealizada();
      case LISTA_PARA_ENTREGAR -> new ListaParaEntregar();
      case EN_TRASLADO -> new EnTraslado();
      case ENTREGADA -> new Entregada();
      case ENTREGA_FALLIDA -> new EntregaFallida();
      case VENCIDA -> new Vencida();
    };
  }
}
