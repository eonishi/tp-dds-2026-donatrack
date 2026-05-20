package ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones;

import ar.edu.utn.donatrack.EstadoDeDonacion.EstadoDonacion;

public class Donacion {
  private static int id;
  private double cantidadTotal;
  private EstadoDonacion estado;

  public void cambiarEstado(EstadoDonacion nuevoEstado, String justificacion){
    estado = nuevoEstado;
  }
}
