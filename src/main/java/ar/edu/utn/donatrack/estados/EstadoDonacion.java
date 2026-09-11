package ar.edu.utn.donatrack.estados;

import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donaciones.HistorialEstado;
import ar.edu.utn.donatrack.donaciones.NombreEstado;
import java.time.LocalDate;

public abstract class EstadoDonacion {
  private final NombreEstado nombre;

  protected EstadoDonacion(NombreEstado nombre) {
    this.nombre = nombre;
  }

  public NombreEstado getNombreEstado() {
    return nombre;
  }

  public void asignar(Donacion donacion) {
    noPermitido("asignar");
  }

  public void planificarRuta(Donacion donacion) {
    noPermitido("planificar la ruta");
  }

  public void iniciarTraslado(Donacion donacion) {
    noPermitido("iniciar el traslado");
  }

  public void confirmarEntrega(Donacion donacion) {
    noPermitido("confirmar la entrega");
  }

  public void reportarFalla(Donacion donacion, String justificacion) {
    noPermitido("reportar una falla");
  }

  public void marcarVencida(Donacion donacion) {
    noPermitido("marcar como vencida");
  }

  public void retornarAlDeposito(Donacion donacion) {
    noPermitido("retornar al depósito");
  }

  protected final void transicionar(
      Donacion donacion, EstadoDonacion destino, String justificacion) {
    donacion.cambiarEstado(
        new HistorialEstado(LocalDate.now(), destino.getNombreEstado(), justificacion), destino);
  }

  private void noPermitido(String operacion) {
    throw new IllegalStateException("No se puede " + operacion + " en estado " + nombre);
  }
}
