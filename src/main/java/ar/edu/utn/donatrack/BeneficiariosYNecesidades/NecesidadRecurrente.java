package ar.edu.utn.donatrack.BeneficiariosYNecesidades;

public class NecesidadRecurrente extends Necesidad {
  TipoPeriodo periodo;

  public NecesidadRecurrente(String descripcion, double cantidadObjetivo, TipoPeriodo periodo) {
    super(descripcion, cantidadObjetivo);
    this.periodo = periodo;
  }

  public void reiniciarPeriodo() {
    setCantidadRecibida(0);
  }
}
