package ar.edu.utn.donatrack.estados;

import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.HistorialEstado;
import ar.edu.utn.donatrack.donaciones.NombreEstado;
import java.time.LocalDateTime;

public class AsignacionRealizada extends EstadoDonacion {
  @Override
  public NombreEstado getNombreEstado() {
    return NombreEstado.ASIGNACION_REALIZADA;
  }

  @Override
  public void asignar(Donacion donacion) {
    throw new IllegalStateException("La donación ya se encuentra asignada.");
  }

  @Override
  public void planificarRuta(Donacion donacion) {
    donacion.cambiarEstado(
        new HistorialEstado(LocalDateTime.now(), NombreEstado.LISTA_PARA_ENTREGAR, null),
        new ListaParaEntregar());
  }

  @Override
  public void iniciarTraslado(Donacion donacion) {
    throw new IllegalStateException(
        "No se puede iniciar el traslado sin haber planificado la ruta previamente.");
  }

  @Override
  public void confirmarEntrega(Donacion donacion) {
    throw new IllegalStateException(
        "No se puede confirmar la entrega de una donación que no está en traslado.");
  }

  @Override
  public void reportarFalla(Donacion donacion, String justificacion) {
    throw new IllegalStateException(
        "No se puede reportar falla para una donación que no está en traslado.");
  }

  @Override
  public void marcarVencida(Donacion donacion) {
    donacion.cambiarEstado(
        new HistorialEstado(LocalDateTime.now(), NombreEstado.VENCIDA, null), new Vencida());
  }

  @Override
  public void retornarAlDeposito(Donacion donacion) {
    throw new IllegalStateException(
        "No se puede retornar al depósito: la donación ya tiene una asignación.");
  }
}
