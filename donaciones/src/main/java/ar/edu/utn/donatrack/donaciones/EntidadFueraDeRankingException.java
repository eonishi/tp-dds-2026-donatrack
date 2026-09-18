package ar.edu.utn.donatrack.donaciones;

public class EntidadFueraDeRankingException extends IllegalStateException {
  public EntidadFueraDeRankingException() {
    super("La entidad no pertenece al ranking vigente.");
  }
}
