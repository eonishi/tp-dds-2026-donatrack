package ar.edu.utn.donatrack.propuestas;

public class EntidadFueraDeRankingException extends IllegalStateException {
  public EntidadFueraDeRankingException() {
    super("La entidad no pertenece al ranking vigente.");
  }
}
