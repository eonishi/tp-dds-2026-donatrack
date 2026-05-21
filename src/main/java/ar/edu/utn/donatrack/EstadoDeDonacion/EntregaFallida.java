package ar.edu.utn.donatrack.EstadoDeDonacion;

import ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones.Donacion;
import ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones.HistorialEstado;
import ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones.NombreEstado;
import java.time.LocalDate;

public class EntregaFallida extends EstadoDonacion {

  @Override
  public void asignar(Donacion donacion, String justificacion) {
    throw new IllegalStateException("La donación ya se encuentra asignada.");
  }

  @Override
  public void planificarRuta(Donacion donacion, String justificacion) {
    donacion.cambiarEstado(
        new HistorialEstado(LocalDate.now(), NombreEstado.LISTA_PARA_ENTREGAR, justificacion),
        new ListaParaEntregar(),
        justificacion
    );
  }

  @Override
  public void iniciarTraslado(Donacion donacion, String justificacion) {
    donacion.cambiarEstado(
        new HistorialEstado(LocalDate.now(), NombreEstado.EN_TRASLADO, justificacion),
        new EnTraslado(),
        justificacion
    );
  }

  @Override
  public void confirmarEntrega(Donacion donacion, String justificacion) {
    throw new IllegalStateException("No se puede confirmar la entrega de una donación con entrega fallida sin reiniciar traslado.");
  }

  @Override
  public void reportarFalla(Donacion donacion, String justificacion) {
    throw new IllegalStateException("La donación ya se encuentra registrada con entrega fallida.");
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
