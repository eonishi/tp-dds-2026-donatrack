package ar.edu.utn.donatrack.donaciones;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.bienes.UnidadMedida;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.logistica.Camion;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

@Entity
public class Donacion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Obligatorio a propósito: sin estado, sería invisible para los filtros y nula al cargar.
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private EstadoDonacion estado = EstadoDonacion.EN_DEPOSITO;

  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "donacion_id")
  private List<HistorialEstado> historialEstado = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "donacion_id")
  private List<Bien> bienes = new ArrayList<>();

  @ManyToOne
  @JoinColumn(name = "entidad_id")
  private EntidadBeneficiaria entidadBeneficiaria;

  @ManyToOne
  @JoinColumn(name = "camion_id")
  private Camion camion;

  @ManyToOne
  @JoinColumn(name = "donante_id")
  private Donante donante;

  protected Donacion() {}

  public Donacion(Donante donante, List<Bien> bienes) {
    this.donante = Objects.requireNonNull(donante, "donante no puede ser null");
    this.bienes.addAll(bienes);
  }

  public void cambiarEstado(EstadoDonacion nuevoEstado, String justificacion) {
    historialEstado.add(new HistorialEstado(LocalDate.now(), nuevoEstado, justificacion));
    estado = nuevoEstado;
  }

  /**
   * Asienta la transición inicial a EN_DEPOSITO cuando se registra la donación.
   *
   * <p>Es idempotente a propósito: una donación que ya transitó antes de ser registrada conserva su
   * propio historial, así que el repositorio puede llamar a este método en cada registro sin
   * agregar una entrada espuria que pasaría a ser la última.
   */
  public void registrarEnDeposito(LocalDate fecha) {
    if (!historialEstado.isEmpty()) {
      return;
    }
    historialEstado.add(new HistorialEstado(fecha, EstadoDonacion.EN_DEPOSITO, null));
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

  public EntidadBeneficiaria getEntidadBeneficiaria() {
    return entidadBeneficiaria;
  }

  public Donante getDonante() {
    return donante;
  }

  public void limpiarEntidadBeneficiaria() {
    this.entidadBeneficiaria = null;
  }

  public Optional<Camion> getCamion() {
    return Optional.ofNullable(this.camion);
  }

  public void asignarCamion(Camion camion) {
    this.camion = Objects.requireNonNull(camion, "camion no debe ser null");
  }

  public void limpiarCamion() {
    this.camion = null;
  }

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
