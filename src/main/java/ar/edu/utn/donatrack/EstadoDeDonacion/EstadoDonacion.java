package ar.edu.utn.donatrack.EstadoDeDonacion;

import ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones.Donacion;

public abstract class EstadoDonacion {

  public abstract void asignar();
  public abstract void planificarRuta();
  public abstract void iniciarTraslado();
  public abstract void confirmarEntrega();
  public abstract void reportarFalla();
  public abstract void marcarVencida();

}
