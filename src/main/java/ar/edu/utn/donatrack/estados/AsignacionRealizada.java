package ar.edu.utn.donatrack.estados;

import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.NombreEstado;

public class AsignacionRealizada extends EstadoDonacion {
  public AsignacionRealizada() {
    super(NombreEstado.ASIGNACION_REALIZADA);
  }

  @Override
  public void planificarRuta(Donacion donacion) {
    transicionar(donacion, new ListaParaEntregar(), null);
  }

  @Override
  public void marcarVencida(Donacion donacion) {
    transicionar(donacion, new Vencida(), null);
  }
}
