package ar.edu.utn.donatrack.EstadoDeDonacion;

import ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones.Donacion;
import ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones.HistorialEstado;
import ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones.NombreEstado;

import java.time.LocalDate;

public class ListaParaEntregar extends EstadoDonacion {
  @Override
  public void asignar(Donacion donacion) {
    throw new IllegalStateException("La donación ya se encuentra asignada.");
  }

  @Override
  public void planificarRuta(Donacion donacion) {
    throw new IllegalStateException("La ruta para esta donación ya ha sido planificada.");
  }

  @Override
  public void iniciarTraslado(Donacion donacion) {
    donacion.cambiarEstado(new HistorialEstado(LocalDate.now(), NombreEstado.EN_TRASLADO, null), new EnTraslado());
  }

  @Override
  public void confirmarEntrega(Donacion donacion) {
    throw new IllegalStateException("No se puede confirmar la entrega sin haber iniciado el traslado.");
  }

  @Override
  public void reportarFalla(Donacion donacion, String justificacion) {
    throw new IllegalStateException("No se puede reportar falla para una donación que no está en traslado.");
  }

  @Override
  public void marcarVencida(Donacion donacion) {
    donacion.cambiarEstado(new HistorialEstado(LocalDate.now(), NombreEstado.VENCIDA, null), new Vencida());
  }
}