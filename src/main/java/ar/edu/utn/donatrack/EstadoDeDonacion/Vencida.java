package ar.edu.utn.donatrack.EstadoDeDonacion;

import ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones.Donacion;

public class Vencida extends EstadoDonacion {

  @Override
  public void asignar(Donacion donacion, String justificacion) {
    throw new IllegalStateException("La donación se encuentra vencida. No admite cambios de estado.");
  }

  @Override
  public void planificarRuta(Donacion donacion, String justificacion) {
    throw new IllegalStateException("La donación se encuentra vencida. No admite cambios de estado.");
  }

  @Override
  public void iniciarTraslado(Donacion donacion, String justificacion) {
    throw new IllegalStateException("La donación se encuentra vencida. No admite cambios de estado.");
  }

  @Override
  public void confirmarEntrega(Donacion donacion, String justificacion) {
    throw new IllegalStateException("La donación se encuentra vencida. No admite cambios de estado.");
  }

  @Override
  public void reportarFalla(Donacion donacion, String justificacion) {
    throw new IllegalStateException("La donación se encuentra vencida. No admite cambios de estado.");
  }

  @Override
  public void marcarVencida(Donacion donacion, String justificacion) {
    throw new IllegalStateException("La donación ya se encuentra vencida.");
  }
}
