package ar.edu.utn.donatrack.EstadoDeDonacion;

import ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones.Donacion;

public abstract class EstadoDonacion {
  public abstract void asignar(Donacion donacion);
  public abstract void planificarRuta(Donacion donacion);
  public abstract void iniciarTraslado(Donacion donacion);
  public abstract void confirmarEntrega(Donacion donacion);
  public abstract void reportarFalla(Donacion donacion, String justificacion);
  public abstract void marcarVencida(Donacion donacion);
}