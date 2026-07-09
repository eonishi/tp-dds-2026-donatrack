package ar.edu.utn.donatrack.donaciones;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.bienes.UnidadMedida;
import ar.edu.utn.donatrack.estados.EnDeposito;
import ar.edu.utn.donatrack.estados.EstadoDonacion;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Donacion {
  private EstadoDonacion estado = new EnDeposito();
  private final List<HistorialEstado> historialEstado = new ArrayList<>();
  private final List<Bien> bienes = new ArrayList<>();
  private ResultadoMatchmaking resultadoMatchmakingVigente;
  private EntidadBeneficiaria entidadBeneficiariaConfirmada;

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

  public double getCantidadTotal() {
    return bienes.stream().mapToDouble(Bien::getCantidad).sum();
  }

  public UnidadMedida getUnidad() {
    return bienes.get(0).getUnidad();
  }

  public boolean esPerecedera() {
    return bienes.get(0).esPerecedero();
  }

  public LocalDate getFechaVencimiento() {
    return bienes.get(0).getFechaVencimiento();
  }

  public ResultadoMatchmaking getResultadoMatchmakingVigente() {
    return resultadoMatchmakingVigente;
  }

  public EntidadBeneficiaria getEntidadBeneficiariaConfirmada() {
    return entidadBeneficiariaConfirmada;
  }

  public void actualizarResultadoMatchmaking(ResultadoMatchmaking resultadoMatchmakingVigente) {
    this.resultadoMatchmakingVigente = resultadoMatchmakingVigente;
  }

  public void confirmarEntidadBeneficiaria(EntidadBeneficiaria entidadBeneficiaria) {
    if (!(estado instanceof EnDeposito)) {
      throw new IllegalStateException(
          "Solo se puede confirmar una entidad para donaciones en depósito.");
    }
    this.entidadBeneficiariaConfirmada = entidadBeneficiaria;
    this.asignar();
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
