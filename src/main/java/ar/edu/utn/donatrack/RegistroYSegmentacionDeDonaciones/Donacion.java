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
    this.justificacion = justificacion;
  }

  public SubCategoria getSubcategoria (){
    return bienes.get(0).getSubcategoria();
  }

  public EstadoDonacion getEstado() {
    return estado;
  }

  public String getJustificacion() {
    return justificacion;
  }

  public List<HistorialEstado> getHistorialEstado() {
    return historialEstado;
  }

  public List<Bien> getBienes() {
    return bienes;
  }

  // Delegación del patrón State
  public void asignar(String justificacion) {
    this.estado.asignar(this, justificacion);
  }

  public void planificarRuta(String justificacion) {
    this.estado.planificarRuta(this, justificacion);
  }

  public void iniciarTraslado(String justificacion) {
    this.estado.iniciarTraslado(this, justificacion);
  }

  public void confirmarEntrega(String justificacion) {
    this.estado.confirmarEntrega(this, justificacion);
  }

  public void reportarFalla(String justificacion) {
    this.estado.reportarFalla(this, justificacion);
  }

  public void marcarVencida(String justificacion) {
    this.estado.marcarVencida(this, justificacion);
  }
}
