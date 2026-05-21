package ar.edu.utn.donatrack.EstadoDeDonacion;

import ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones.Donacion;

public abstract class EstadoDonacion {

  public abstract void asignar(Donacion donacion, String justificacion);
  public abstract void planificarRuta(Donacion donacion, String justificacion);
  public abstract void iniciarTraslado(Donacion donacion, String justificacion);
  public abstract void confirmarEntrega(Donacion donacion, String justificacion);
  public abstract void reportarFalla(Donacion donacion, String justificacion);
  public abstract void marcarVencida(Donacion donacion, String justificacion);

}
