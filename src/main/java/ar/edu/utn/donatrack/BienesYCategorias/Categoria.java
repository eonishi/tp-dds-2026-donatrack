package ar.edu.utn.donatrack.BienesYCategorias;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Categoria {
  private String nombre;
  private boolean requiereEstadoUso;
  private List<SubCategoria> subCategorias = new ArrayList<>();

  public Categoria(String nombre, boolean requiereEstadoUso) {
    this.nombre = nombre;
    this.requiereEstadoUso = requiereEstadoUso;
  }

  public void agregarSubCategoria(SubCategoria subCategoria){
    this.subCategorias.add(subCategoria);
  }

  public List<SubCategoria> getSubCategorias() {
    return Collections.unmodifiableList(subCategorias);
  }
}