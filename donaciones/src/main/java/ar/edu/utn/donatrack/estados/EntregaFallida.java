package ar.edu.utn.donatrack.estados;

import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.HistorialEstado;
import ar.edu.utn.donatrack.donaciones.NombreEstado;
import java.time.LocalDateTime;

public class EntregaFallida extends EstadoDonacion {
  @Override
  public NombreEstado getNombreEstado() {
    return NombreEstado.ENTREGA_FALLIDA;
  }

  @Override
  public void asignar(Donacion donacion) {
    throw new IllegalStateException("La donación ya se encuentra asignada.");
  }

  @Override
  public void planificarRuta(Donacion donacion) {
    throw new IllegalStateException(
        "No se puede planificar ruta desde una entrega fallida. Use retornarAlDeposito primero.");
  }

  @Override
  public void iniciarTraslado(Donacion donacion) {
    throw new IllegalStateException("Debe planificarse la ruta nuevamente.");
  }

  @Override
  public void confirmarEntrega(Donacion donacion) {
    throw new IllegalStateException(
        "No se puede confirmar la entrega de una donación con entrega fallida.");
  }

  @Override
  public void reportarFalla(Donacion donacion, String justificacion) {
    throw new IllegalStateException("La donación ya se encuentra registrada con entrega fallida.");
  }

  @Override
  public void marcarVencida(Donacion donacion) {
    donacion.cambiarEstado(
        new HistorialEstado(LocalDateTime.now(), NombreEstado.VENCIDA, null), new Vencida());
  }

  @Override
  public void retornarAlDeposito(Donacion donacion) {
    donacion.limpiarEntidadBeneficiaria();
    // ── LÓGICA FALTANTE — microservicio de LOGÍSTICA ──────────────────────
    // Antes de volver al depósito, el camión asignado debe desvincularse en
    // el microservicio de logística. No migrada: requiere integración REST
    // con el microservicio de logística.
    //
    //   donacion.limpiarCamion();
    //
    donacion.cambiarEstado(
        new HistorialEstado(LocalDateTime.now(), NombreEstado.EN_DEPOSITO, null), new EnDeposito());
  }
}
