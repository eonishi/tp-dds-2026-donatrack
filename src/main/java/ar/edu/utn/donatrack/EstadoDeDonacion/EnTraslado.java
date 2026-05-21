package ar.edu.utn.donatrack.EstadoDeDonacion;

import ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones.Donacion;
import ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones.HistorialEstado;
import ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones.NombreEstado;
import java.time.LocalDate;

public class EnTraslado extends EstadoDonacion {

  @Override
  public void asignar(Donacion donacion, String justificacion) {
    throw new IllegalStateException("La donación ya se encuentra asignada.");
  }

  @Override
  public void planificarRuta(Donacion donacion, String justificacion) {
    throw new IllegalStateException("No se puede planificar la ruta de una donación que ya está en traslado.");
  }

  @Override
  public void iniciarTraslado(Donacion donacion, String justificacion) {
    throw new IllegalStateException("El traslado ya se encuentra en curso.");
  }

  @Override
  public void confirmarEntrega(Donacion donacion, String justificacion) {
    donacion.cambiarEstado(
        new HistorialEstado(LocalDate.now(), NombreEstado.ENTREGADA, justificacion),
        new Entregada(),
        justificacion
    );
  }

  @Override
  public void reportarFalla(Donacion donacion, String justificacion) {
    donacion.cambiarEstado(
        new HistorialEstado(LocalDate.now(), NombreEstado.ENTREGA_FALLIDA, justificacion),
        new EntregaFallida(),
        justificacion
    );
  }

  @Override
  public void marcarVencida(Donacion donacion, String justificacion) {
    donacion.cambiarEstado(
        new HistorialEstado(LocalDate.now(), NombreEstado.VENCIDA, justificacion),
        new Vencida(),
        justificacion
    );
  }
}
