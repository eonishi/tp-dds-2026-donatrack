package ar.edu.utn.donatrack.donaciones;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.bienes.UnidadMedida;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.estados.EnDeposito;
import ar.edu.utn.donatrack.estados.EstadoDonacion;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Donacion {
  private EstadoDonacion estado = new EnDeposito();
  private final List<HistorialEstado> historialEstado = new ArrayList<>();
  private final List<Bien> bienes = new ArrayList<>();
  private EntidadBeneficiaria entidadBeneficiaria;
  /*
   * ── LÓGICA FALTANTE — microservicio de LOGÍSTICA ──────────────────────
   * La asignación de un camión a la donación vive en el microservicio de
   * logística. No migrada: la integración REST entre servicios debe
   * actualizar el estado de la donación (planificarRuta/iniciarTraslado)
   * a partir de los eventos del microservicio de logística.
   *
   *   private Camion camion;
   */
  private final Donante donante;

  public Donacion(Donante donante, List<Bien> bienes) {
    this.donante = Objects.requireNonNull(donante, "donante no puede ser null");
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

  public NombreEstado getNombreEstado() {
    return estado.getNombreEstado();
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

  public EntidadBeneficiaria getEntidadBeneficiaria() {
    return entidadBeneficiaria;
  }

  public Donante getDonante() {
    return donante;
  }

  public void limpiarEntidadBeneficiaria() {
    this.entidadBeneficiaria = null;
  }

  /*
   * ── LÓGICA FALTANTE — microservicio de LOGÍSTICA ──────────────────────
   * El camión asignado a la donación lo conoce el microservicio de
   * logística (Ruta/Camion). No migrada: el microservicio de donaciones
   * debe consultar/recibir el camión por integración REST o eventos.
   *
   *   public Optional<Camion> getCamion() {
   *     return Optional.ofNullable(this.camion);
   *   }
   *
   *   public void asignarCamion(Camion camion) {
   *     this.camion = Objects.requireNonNull(camion, "camion no debe ser null");
   *   }
   *
   *   public void limpiarCamion() {
   *     this.camion = null;
   *   }
   */

  public void setEntidadBeneficiaria(EntidadBeneficiaria entidadBeneficiaria) {
    this.entidadBeneficiaria = entidadBeneficiaria;
  }

  public void confirmarEntidadBeneficiaria(EntidadBeneficiaria entidadBeneficiaria) {
    this.setEntidadBeneficiaria(entidadBeneficiaria);
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

  public void retornarAlDeposito() {
    this.estado.retornarAlDeposito(this);
  }
}
