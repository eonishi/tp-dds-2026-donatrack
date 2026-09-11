package ar.edu.utn.donatrack.estados;

import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.NombreEstado;

public class EntregaFallida extends EstadoDonacion {
  public EntregaFallida() {
    super(NombreEstado.ENTREGA_FALLIDA);
  }

  @Override
  public void marcarVencida(Donacion donacion) {
    transicionar(donacion, new Vencida(), null);
  }

  @Override
  public void retornarAlDeposito(Donacion donacion) {
    donacion.limpiarEntidadBeneficiaria();
    donacion.limpiarCamion();
    transicionar(donacion, new EnDeposito(), null);
  }
}
