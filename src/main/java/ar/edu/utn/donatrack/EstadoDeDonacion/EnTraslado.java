package ar.edu.utn.donatrack.EstadoDeDonacion;

import ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones.Donacion;
import ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones.HistorialEstado;
import ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones.NombreEstado;

import java.time.LocalDate;

public class EnTraslado extends EstadoDonacion {
  @Override
  public void asignar(Donacion donacion) {
    throw new IllegalStateException("La donación ya se encuentra asignada.");
  }

  @Override
  public void planificarRuta(Donacion donacion) {
    throw new IllegalStateException("No se puede planificar la ruta de una donación que ya está en traslado.");
  }

  @Override
  public void iniciarTraslado(Donacion donacion) {
    throw new IllegalStateException("El traslado ya se encuentra en curso.");
  }

  @Override
  public void confirmarEntrega(Donacion donacion) {
    donacion.cambiarEstado(new HistorialEstado(LocalDate.now(), NombreEstado.ENTREGADA, null), new Entregada());
  }

  @Override
  public void reportarFalla(Donacion donacion, String justificacion) {
    donacion.cambiarEstado(new HistorialEstado(LocalDate.now(), NombreEstado.ENTREGA_FALLIDA, justificacion), new EntregaFallida());
  }

  @Override
  public void marcarVencida(Donacion donacion) {
    donacion.cambiarEstado(new HistorialEstado(LocalDate.now(), NombreEstado.VENCIDA, null), new Vencida());
  }
}