package ar.edu.utn.donatrack.BienesYCategorias;

import java.util.ArrayList;
import java.util.List;

public class Categoria {
  private String nombre;
  private boolean requiereEstadoUso;
  private List<SubCategoria> subCategoria = new ArrayList<>();


  public Categoria(String nombre, boolean requiereEstadoUso) {
    this.nombre = nombre;
    this.requiereEstadoUso = requiereEstadoUso;
  }

  public void agregarCategoria(SubCategoria subCategoria){
    this.subCategoria.add(subCategoria);
  }
}
