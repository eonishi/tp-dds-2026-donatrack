package ar.edu.utn.donatrack.estados;

import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.NombreEstado;

public class Vencida extends EstadoDonacion {
  @Override
  public NombreEstado getNombreEstado() {
    return NombreEstado.VENCIDA;
  }

  @Override
  public void asignar(Donacion donacion) {
    throw new IllegalStateException(
        "La donación se encuentra vencida. No admite cambios de estado.");
  }

  @Override
  public void planificarRuta(Donacion donacion) {
    throw new IllegalStateException(
        "La donación se encuentra vencida. No admite cambios de estado.");
  }

  @Override
  public void iniciarTraslado(Donacion donacion) {
    throw new IllegalStateException(
        "La donación se encuentra vencida. No admite cambios de estado.");
  }

  @Override
  public void confirmarEntrega(Donacion donacion) {
    throw new IllegalStateException(
        "La donación se encuentra vencida. No admite cambios de estado.");
  }

  @Override
  public void reportarFalla(Donacion donacion, String justificacion) {
    throw new IllegalStateException(
        "La donación se encuentra vencida. No admite cambios de estado.");
  }

  @Override
  public void marcarVencida(Donacion donacion) {
    throw new IllegalStateException("La donación ya se encuentra vencida.");
  }

  @Override
  public void retornarAlDeposito(Donacion donacion) {
    throw new IllegalStateException(
        "La donación se encuentra vencida. No admite cambios de estado.");
  }
}
