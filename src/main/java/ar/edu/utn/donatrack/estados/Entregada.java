package ar.edu.utn.donatrack.estados;

import ar.edu.utn.donatrack.donaciones.Donacion;

public class Entregada extends EstadoDonacion {
  @Override
  public void asignar(Donacion donacion) {
    throw new IllegalStateException(
        "La donación ya ha sido entregada. No admite cambios de estado.");
  }

  @Override
  public void planificarRuta(Donacion donacion) {
    throw new IllegalStateException(
        "La donación ya ha sido entregada. No admite cambios de estado.");
  }

  @Override
  public void iniciarTraslado(Donacion donacion) {
    throw new IllegalStateException(
        "La donación ya ha sido entregada. No admite cambios de estado.");
  }

  @Override
  public void confirmarEntrega(Donacion donacion) {
    throw new IllegalStateException(
        "La donación ya ha sido entregada. No admite cambios de estado.");
  }

  @Override
  public void reportarFalla(Donacion donacion, String justificacion) {
    throw new IllegalStateException(
        "La donación ya ha sido entregada. No admite cambios de estado.");
  }

  @Override
  public void marcarVencida(Donacion donacion) {
    throw new IllegalStateException(
        "La donación ya ha sido entregada. No admite cambios de estado.");
  }

  @Override
  public void retornarAlDeposito(Donacion donacion) {
    throw new IllegalStateException(
        "La donación ya ha sido entregada. No admite cambios de estado.");
  }
}
