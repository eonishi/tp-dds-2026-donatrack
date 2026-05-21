package ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones;

import ar.edu.utn.donatrack.BienesYCategorias.Bien;
import ar.edu.utn.donatrack.BienesYCategorias.SubCategoria;
import ar.edu.utn.donatrack.EstadoDeDonacion.EnDeposito;
import ar.edu.utn.donatrack.EstadoDeDonacion.EstadoDonacion;
import org.w3c.dom.html.HTMLDOMImplementation;
import java.util.ArrayList;
import java.util.List;


public class Donacion {
  private EstadoDonacion estado = new EnDeposito();
  private String justificacion = "Sin comentarios";
  private List<HistorialEstado> historialEstado = new ArrayList<>();
  private List<Bien> bienes = new ArrayList<>();



  //Constructor
  public Donacion(List<Bien> bienes) {
    this.bienes.addAll(bienes);
  }


  public void setJustificacion(String justificacion) {
    this.justificacion = justificacion;
  }

  public void cambiarEstado(HistorialEstado nuevoHistorial, EstadoDonacion NuevoEstado, String justificacion){
    historialEstado.add(nuevoHistorial);
    estado = NuevoEstado;
  }

  public SubCategoria getSubcategoria (){
    return bienes.get(0).getSubcategoria();
  }
}
