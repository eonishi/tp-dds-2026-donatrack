package ar.edu.utn.donatrack.EstadoDeDonacion;

import ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones.Donacion;
import ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones.HistorialEstado;
import ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones.NombreEstado;
import java.time.LocalDate;

public class EnDeposito extends EstadoDonacion {
  @Override
  public void asignar(Donacion donacion) {
    donacion.cambiarEstado(new HistorialEstado(LocalDate.now(), NombreEstado.ASIGNACION_REALIZADA, null), new AsignacionRealizada());
  }

  @Override
  public void planificarRuta(Donacion donacion) { throw new IllegalStateException("No se puede planificar la ruta de una donación en depósito sin asignar."); }

  @Override
  public void iniciarTraslado(Donacion donacion) { throw new IllegalStateException("No se puede iniciar el traslado de una donación en depósito."); }

  @Override
  public void confirmarEntrega(Donacion donacion) { throw new IllegalStateException("No se puede confirmar la entrega de una donación en depósito."); }

  @Override
  public void reportarFalla(Donacion donacion, String justificacion) { throw new IllegalStateException("No se puede reportar falla de entrega para una donación en depósito."); }

  @Override
  public void marcarVencida(Donacion donacion) {
    donacion.cambiarEstado(new HistorialEstado(LocalDate.now(), NombreEstado.VENCIDA, null), new Vencida());
  }
}