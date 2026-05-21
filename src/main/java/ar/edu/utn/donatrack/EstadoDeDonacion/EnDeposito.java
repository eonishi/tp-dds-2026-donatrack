package ar.edu.utn.donatrack.EstadoDeDonacion;

import ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones.Donacion;
import ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones.HistorialEstado;
import ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones.NombreEstado;
import java.time.LocalDate;

public class EnDeposito extends EstadoDonacion {

  @Override
  public void asignar(Donacion donacion, String justificacion) {
    donacion.cambiarEstado(
        new HistorialEstado(LocalDate.now(), NombreEstado.ASIGNACION_REALIZADA, justificacion),
        new AsignacionRealizada(),
        justificacion
    );
  }

  @Override
  public void planificarRuta(Donacion donacion, String justificacion) {
    throw new IllegalStateException("No se puede planificar la ruta de una donación en depósito sin asignar.");
  }

  @Override
  public void iniciarTraslado(Donacion donacion, String justificacion) {
    throw new IllegalStateException("No se puede iniciar el traslado de una donación en depósito.");
  }

  @Override
  public void confirmarEntrega(Donacion donacion, String justificacion) {
    throw new IllegalStateException("No se puede confirmar la entrega de una donación en depósito.");
  }

  @Override
  public void reportarFalla(Donacion donacion, String justificacion) {
    throw new IllegalStateException("No se puede reportar falla de entrega para una donación en depósito.");
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
