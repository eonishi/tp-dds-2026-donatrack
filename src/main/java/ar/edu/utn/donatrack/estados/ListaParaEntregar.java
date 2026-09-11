package ar.edu.utn.donatrack.estados;

import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.NombreEstado;

public class ListaParaEntregar extends EstadoDonacion {
  public ListaParaEntregar() {
    super(NombreEstado.LISTA_PARA_ENTREGAR);
  }

  @Override
  public void iniciarTraslado(Donacion donacion) {
    transicionar(donacion, new EnTraslado(), null);
  }

  @Override
  public void marcarVencida(Donacion donacion) {
    transicionar(donacion, new Vencida(), null);
  }
}
