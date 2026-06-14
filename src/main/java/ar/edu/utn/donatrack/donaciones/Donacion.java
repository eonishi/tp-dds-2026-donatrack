package ar.edu.utn.donatrack.donaciones;

import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.estados.EnDeposito;
import ar.edu.utn.donatrack.estados.EstadoDonacion;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Donacion {
  private EstadoDonacion estado = new EnDeposito();
  private final List<HistorialEstado> historialEstado = new ArrayList<>();
  private final List<Bien> bienes = new ArrayList<>();

  public Donacion(List<Bien> bienes) {
    this.bienes.addAll(bienes);
  }

  public void cambiarEstado(HistorialEstado nuevoHistorial, EstadoDonacion nuevoEstado) {
    historialEstado.add(nuevoHistorial);
    estado = nuevoEstado;
  }

  public SubCategoria getSubcategoria() {
    return bienes.get(0).getSubcategoria();
  }

  public EstadoDonacion getEstado() {
    return estado;
  }

  public List<HistorialEstado> getHistorialEstado() {
    return Collections.unmodifiableList(historialEstado);
  }

  public List<Bien> getBienes() {
    return Collections.unmodifiableList(bienes);
  }

  public void asignar() {
    this.estado.asignar(this);
  }

  public void planificarRuta() {
    this.estado.planificarRuta(this);
  }

  public void iniciarTraslado() {
    this.estado.iniciarTraslado(this);
  }

  public void confirmarEntrega() {
    this.estado.confirmarEntrega(this);
  }

  public void reportarFalla(String justificacion) {
    this.estado.reportarFalla(this, justificacion);
  }

  public void marcarVencida() {
    this.estado.marcarVencida(this);
  }
}
