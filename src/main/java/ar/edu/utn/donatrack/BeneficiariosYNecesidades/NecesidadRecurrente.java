package ar.edu.utn.donatrack.BeneficiariosYNecesidades;

public class NecesidadRecurrente extends Necesidad{

  TipoPeriodo periodo;

  @Override
  public boolean estaSatisfecha() {
    return false;
  }

  public void reiniciarPeriodo(){

  }
}
