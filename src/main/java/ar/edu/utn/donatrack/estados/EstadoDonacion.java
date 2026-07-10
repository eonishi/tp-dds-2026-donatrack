package ar.edu.utn.donatrack.estados;

import ar.edu.utn.donatrack.donaciones.Donacion;

public abstract class EstadoDonacion {
  public abstract void asignar(Donacion donacion);

  public abstract void planificarRuta(Donacion donacion);

  public abstract void iniciarTraslado(Donacion donacion);

  public abstract void confirmarEntrega(Donacion donacion);

  public abstract void reportarFalla(Donacion donacion, String justificacion);

  public abstract void marcarVencida(Donacion donacion);

  public abstract void retornarAlDeposito(Donacion donacion);
}
