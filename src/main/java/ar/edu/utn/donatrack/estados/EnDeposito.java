package ar.edu.utn.donatrack.estados;

import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.NombreEstado;

public class EnDeposito extends EstadoDonacion {
  public EnDeposito() {
    super(NombreEstado.EN_DEPOSITO);
  }

  @Override
  public void asignar(Donacion donacion) {
    transicionar(donacion, new AsignacionRealizada(), null);
  }

  @Override
  public void marcarVencida(Donacion donacion) {
    transicionar(donacion, new Vencida(), null);
  }
}
