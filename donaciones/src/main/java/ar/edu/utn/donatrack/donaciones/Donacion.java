package ar.edu.utn.donatrack.donaciones;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.bienes.UnidadMedida;
import ar.edu.utn.donatrack.donantes.Donante;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

@Entity
public class Donacion {
  @Id private UUID id;

  // Obligatorio a propósito: sin estado, sería invisible para los filtros y nula al cargar.
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private EstadoDonacion estado = EstadoDonacion.EN_DEPOSITO;

  // El historial es append-only: el orden de la colección es el de inserción de sus entradas. La
  // columna de orden lo conserva al recargar y hace que agregar una entrada sea un solo insert: sin
  // ella, Hibernate borra y reinserta la colección entera en cada cambio.
  @ElementCollection
  @CollectionTable(name = "HistorialEstado", joinColumns = @JoinColumn(name = "donacion_id"))
  @OrderColumn(name = "orden")
  private List<HistorialEstado> historialEstado = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "donacion_id")
  private List<Bien> bienes = new ArrayList<>();

  @ElementCollection
  @CollectionTable(name = "donacion_fotos", joinColumns = @JoinColumn(name = "donacion_id"))
  @Column(name = "foto")
  private List<String> fotos = new ArrayList<>();

  @ManyToOne
  @JoinColumn(name = "entidad_id")
  private EntidadBeneficiaria entidadBeneficiaria;

  private String idCamion;

  @ManyToOne
  @JoinColumn(name = "donante_id")
  private Donante donante;

  protected Donacion() {}

  public Donacion(DonacionId id, Donante donante, List<Bien> bienes) {
    this.id = Objects.requireNonNull(id, "id no puede ser null").value();
    this.donante = Objects.requireNonNull(donante, "donante no puede ser null");
    this.bienes.addAll(bienes);
  }

  /**
   * Devuelve el identificador de la donación.
   *
   * @return el identificador de la donación
   */
  public DonacionId getId() {
    return new DonacionId(id);
  }

  /**
   * Registra una transición de estado construyendo la entrada de historial que la respalda.
   *
   * <p>El historial es interno a la donación: quien transiciona indica el estado destino y la
   * justificación, y la donación arma la entrada con el instante que corresponde. El instante lo
   * fija la donación y no el llamador: la transición ocurre cuando se ejecuta, y dejar que un
   * tercero la feche permitiría un historial cuya fecha contradiga el orden real de los eventos.
   *
   * @param nuevoEstado estado al que transiciona la donación (no nulo)
   * @param justificacion motivo de la transición, o {@code null} si no requiere justificación
   * @throws NullPointerException si {@code nuevoEstado} es {@code null}
   */
  public synchronized void cambiarEstado(EstadoDonacion nuevoEstado, String justificacion) {
    Objects.requireNonNull(nuevoEstado, "estado no puede ser null");
    // Microsegundos: es la precisión de la base, así la fecha en memoria es la que se persiste.
    LocalDateTime instante = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
    historialEstado.add(new HistorialEstado(instante, nuevoEstado, justificacion));
    estado = nuevoEstado;
  }

  /**
   * Reemplaza los bienes de la donación conservando una copia defensiva de la lista recibida.
   *
   * @param nuevosBienes bienes que pasan a componer la donación (no vacíos ni nulos)
   * @throws IllegalArgumentException si la lista es nula, está vacía o contiene un bien nulo
   */
  public void reemplazarBienes(List<Bien> nuevosBienes) {
    if (nuevosBienes == null
        || nuevosBienes.isEmpty()
        || nuevosBienes.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("La donación debe contener al menos un bien válido.");
    }
    this.bienes.clear();
    this.bienes.addAll(nuevosBienes);
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

  public List<String> getFotos() {
    return Collections.unmodifiableList(fotos);
  }

  public void agregarFoto(String referencia) {
    if (referencia == null || referencia.isBlank()) {
      throw new IllegalArgumentException("La referencia de la foto es obligatoria.");
    }
    fotos.add(referencia.trim());
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

  /**
   * Registra la patente del camión que retira la donación.
   *
   * <p>La patente llega como referencia de valor desde el microservicio de logística (la flota es
   * de logística); se registra al iniciar el traslado y no se vuelve a pedir al confirmar la
   * entrega.
   *
   * @param patente patente del camión (no nula)
   * @throws NullPointerException si {@code patente} es {@code null}
   */
  public void registrarCamion(String patente) {
    this.idCamion = Objects.requireNonNull(patente, "idCamion no debe ser null");
  }

  /**
   * @return la patente del camión asignado, o {@link Optional#empty()} si la donación no tiene
   *     camión
   */
  public Optional<String> getIdCamion() {
    return Optional.ofNullable(idCamion);
  }

  /** Desvincula el camión asignado, cuando la donación vuelve al depósito. */
  public void limpiarCamion() {
    this.idCamion = null;
  }

  public void setEntidadBeneficiaria(EntidadBeneficiaria entidadBeneficiaria) {
    this.entidadBeneficiaria = entidadBeneficiaria;
  }

  public void confirmarEntidadBeneficiaria(EntidadBeneficiaria entidadBeneficiaria) {
    Objects.requireNonNull(entidadBeneficiaria, "entidadBeneficiaria no puede ser null");
    this.asignar();
    this.setEntidadBeneficiaria(entidadBeneficiaria);
  }

  public List<ar.edu.utn.donatrack.notificaciones.Notificacion> asignar() {
    return this.estado.asignar(this);
  }

  public List<ar.edu.utn.donatrack.notificaciones.Notificacion> planificarRuta() {
    return this.estado.planificarRuta(this);
  }

  public List<ar.edu.utn.donatrack.notificaciones.Notificacion> iniciarTraslado(String urlMapa) {
    return this.estado.iniciarTraslado(this, urlMapa);
  }

  public List<ar.edu.utn.donatrack.notificaciones.Notificacion> confirmarEntrega() {
    return this.estado.confirmarEntrega(this);
  }

  public List<ar.edu.utn.donatrack.notificaciones.Notificacion> reportarFalla(
      String justificacion) {
    if (justificacion == null || justificacion.isBlank()) {
      throw new IllegalArgumentException("La justificación es obligatoria.");
    }
    return this.estado.reportarFalla(this, justificacion);
  }

  public List<ar.edu.utn.donatrack.notificaciones.Notificacion> marcarVencida() {
    return this.estado.marcarVencida(this);
  }

  public List<ar.edu.utn.donatrack.notificaciones.Notificacion> retornarAlDeposito() {
    return this.estado.retornarAlDeposito(this);
  }
}
