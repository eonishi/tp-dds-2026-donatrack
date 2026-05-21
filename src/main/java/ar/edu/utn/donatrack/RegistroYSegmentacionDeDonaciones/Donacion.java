package ar.edu.utn.donatrack.RegistroYSegmentacionDeDonaciones;

import ar.edu.utn.donatrack.EstadoDeDonacion.EstadoDonacion;
import org.w3c.dom.html.HTMLDOMImplementation;
import java.util.ArrayList;
import java.util.List;


public class Donacion {
  private int id;
  private double cantidadTotal;
  private EstadoDonacion estado;
  private String justificacion = "Sin comentarios";
  private List<HistorialEstado> historialEstado = new ArrayList<>();


  //Constructor
  public Donacion(int id, double cantidadTotal, EstadoDonacion estado) {
    this.id = id;
    this.cantidadTotal = cantidadTotal;
    this.estado = estado;
  }


  public void setJustificacion(String justificacion) {
    this.justificacion = justificacion;
  }

  public void cambiarEstado(HistorialEstado nuevoHistorial, EstadoDonacion NuevoEstado, String justificacion){
    historialEstado.add(nuevoHistorial);
    estado = NuevoEstado;
  }
}
