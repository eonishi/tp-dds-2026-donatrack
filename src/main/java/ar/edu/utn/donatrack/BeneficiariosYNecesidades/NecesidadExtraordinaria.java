package ar.edu.utn.donatrack.BeneficiariosYNecesidades;

import ar.edu.utn.donatrack.BienesYCategorias.Bien;

public class NecesidadExtraordinaria extends Necesidad {
  public NecesidadExtraordinaria(Bien bien) {
    super(bien);
  }

  @Override
  public boolean estaSatisfecha() {
    return super.estaSatisfecha();
  }
}