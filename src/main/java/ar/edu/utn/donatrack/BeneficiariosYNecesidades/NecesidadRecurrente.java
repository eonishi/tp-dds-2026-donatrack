package ar.edu.utn.donatrack.BeneficiariosYNecesidades;

import ar.edu.utn.donatrack.BienesYCategorias.Bien;

public class NecesidadRecurrente extends Necesidad {
  private TipoPeriodo periodo;

  public NecesidadRecurrente(Bien bien) {
    super(bien);
  }

  public TipoPeriodo getPeriodo() { return periodo; }
  public void setPeriodo(TipoPeriodo periodo) { this.periodo = periodo; }

  @Override
  public boolean estaSatisfecha() {
    return super.estaSatisfecha();
  }

  public void reiniciarPeriodo(){
    this.resetearCantidadRecibida(); 
  }
}