package ar.edu.utn.donatrack.estados;

import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.HistorialEstado;
import ar.edu.utn.donatrack.donaciones.NombreEstado;
import java.time.LocalDate;

public class EnTraslado extends EstadoDonacion {
  @Override
  public NombreEstado getNombreEstado() {
    return NombreEstado.EN_TRASLADO;
  }

  @Override
  public void asignar(Donacion donacion) {
    throw new IllegalStateException("La donación ya se encuentra asignada.");
  }

  @Override
  public void planificarRuta(Donacion donacion) {
    throw new IllegalStateException(
        "No se puede planificar la ruta de una donación que ya está en traslado.");
  }

  @Override
  public void iniciarTraslado(Donacion donacion) {
    throw new IllegalStateException("El traslado ya se encuentra en curso.");
  }

  @Override
  public void confirmarEntrega(Donacion donacion) {
    donacion.cambiarEstado(
        new HistorialEstado(LocalDate.now(), NombreEstado.ENTREGADA, null), new Entregada());
    donacion.getEntidadBeneficiaria().registrarRecepcion(donacion, LocalDate.now());
  }

  @Override
  public void reportarFalla(Donacion donacion, String justificacion) {
    donacion.cambiarEstado(
        new HistorialEstado(LocalDate.now(), NombreEstado.ENTREGA_FALLIDA, justificacion),
        new EntregaFallida());
  }

  @Override
  public void marcarVencida(Donacion donacion) {
    donacion.cambiarEstado(
        new HistorialEstado(LocalDate.now(), NombreEstado.VENCIDA, null), new Vencida());
  }

  @Override
  public void retornarAlDeposito(Donacion donacion) {
    throw new IllegalStateException(
        "No se puede retornar al depósito: la donación está en traslado.");
  }
}
