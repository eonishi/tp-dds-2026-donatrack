package ar.edu.utn.donatrack.EstadoDeDonacion;

import ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones.Donacion;
import ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones.HistorialEstado;
import ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones.NombreEstado;

import java.time.LocalDate;

public class EntregaFallida extends EstadoDonacion {
  @Override
  public void asignar(Donacion donacion) {
    throw new IllegalStateException("La donación ya se encuentra asignada.");
  }

  @Override
  public void planificarRuta(Donacion donacion) {
    // Al fallar y volver al depósito, se puede volver a planificar ruta.
    donacion.cambiarEstado(new HistorialEstado(LocalDate.now(), NombreEstado.LISTA_PARA_ENTREGAR, null), new ListaParaEntregar());
  }

  @Override
  public void iniciarTraslado(Donacion donacion) {
    throw new IllegalStateException("Debe planificarse la ruta nuevamente.");
  }

  @Override
  public void confirmarEntrega(Donacion donacion) {
    throw new IllegalStateException("No se puede confirmar la entrega de una donación con entrega fallida.");
  }

  @Override
  public void reportarFalla(Donacion donacion, String justificacion) {
    throw new IllegalStateException("La donación ya se encuentra registrada con entrega fallida.");
  }

  @Override
  public void marcarVencida(Donacion donacion) {
    donacion.cambiarEstado(new HistorialEstado(LocalDate.now(), NombreEstado.VENCIDA, null), new Vencida());
  }
}