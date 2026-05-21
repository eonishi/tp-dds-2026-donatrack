package ar.edu.utn.donatrack.BeneficiariosYNecesidades;

public class NecesidadRecurrente extends Necesidad{

  TipoPeriodo periodo;

  public TipoPeriodo getPeriodo() {
    return periodo;
  }

  public void setPeriodo(TipoPeriodo periodo) {
    this.periodo = periodo;
  }

  @Override
  public boolean estaSatisfecha() {
    return super.estaSatisfecha();
  }

  public void reiniciarPeriodo(){
    this.setCantidadRecibida(0.0);
  }
}
