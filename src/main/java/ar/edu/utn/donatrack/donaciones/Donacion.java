package ar.edu.utn.donatrack.donaciones;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.bienes.UnidadMedida;
import ar.edu.utn.donatrack.donantes.Donante;
import ar.edu.utn.donatrack.estados.EnDeposito;
import ar.edu.utn.donatrack.estados.EstadoDonacion;
import ar.edu.utn.donatrack.logistica.Camion;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.Transient;

@Entity
public class Donacion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Transient private EstadoDonacion estado = new EnDeposito();

  // Eager and non-final on purpose: the state is derived from this collection once the donation is
  // loaded, so it must already be there, and Hibernate must be able to replace the field.
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
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

  public void cambiarEstado(HistorialEstado nuevoHistorial, EstadoDonacion nuevoEstado) {
    historialEstado.add(nuevoHistorial);
    estado = nuevoEstado;
  }

  /**
   * Records the initial EN_DEPOSITO transition when the donation is registered.
   *
   * <p>It is idempotent on purpose: a donation that already transitioned before being registered
   * keeps its own history, so the repository may call this on every registration without adding a
   * spurious entry that would become the latest one.
   */
  public void registrarEnDeposito(LocalDate fecha) {
    if (!historialEstado.isEmpty()) {
      return;
    }
    historialEstado.add(new HistorialEstado(fecha, NombreEstado.EN_DEPOSITO, null));
  }

  /**
   * The state is not persisted: it is derived from the latest transition recorded in the historial,
   * understood as the entry with the highest id. Ordering by {@code fecha} would be wrong, since it
   * is a date and two transitions can share the same day. Without history entries the state keeps
   * its current value.
   */
  @PostLoad
  private void derivarEstadoDelHistorial() {
    historialEstado.stream()
        .max(Comparator.comparing(HistorialEstado::getId))
        .ifPresent(ultimo -> estado = ultimo.getEstadoAlcanzado().crearEstado());
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
