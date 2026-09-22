package ar.edu.utn.donatrack.donaciones;

import ar.edu.utn.donatrack.beneficiarios.EntidadBeneficiaria;
import ar.edu.utn.donatrack.bienes.Bien;
import ar.edu.utn.donatrack.bienes.SubCategoria;
import ar.edu.utn.donatrack.bienes.UnidadMedida;
import ar.edu.utn.donatrack.donantes.Donante;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Donacion {
  private final DonacionId id;
  private EstadoDonacion estado = EstadoDonacion.EN_DEPOSITO;
  private final List<HistorialEstado> historialEstado = new ArrayList<>();
  private final List<Bien> bienes = new ArrayList<>();
  private final List<String> fotos = new ArrayList<>();
  private EntidadBeneficiaria entidadBeneficiaria;
  private String idCamion;
  private final Donante donante;

  public Donacion(DonacionId id, Donante donante, List<Bien> bienes) {
    this.id = Objects.requireNonNull(id, "id no puede ser null");
    this.donante = Objects.requireNonNull(donante, "donante no puede ser null");
    this.bienes.addAll(bienes);
  }

  /**
   * Devuelve el identificador de la donación.
   *
   * @return el identificador de la donación
   */
  public DonacionId getId() {
    return id;
  }

  public synchronized void cambiarEstado(
      HistorialEstado nuevoHistorial, EstadoDonacion nuevoEstado) {
    Objects.requireNonNull(nuevoHistorial, "historial no puede ser null");
    Objects.requireNonNull(nuevoEstado, "estado no puede ser null");
    historialEstado.add(nuevoHistorial.contextualizar(historialEstado.size() + 1L, estado));
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
    if (justificacion == null || justificacion.isBlank()) {
      throw new IllegalArgumentException("La justificación es obligatoria.");
    }
    this.estado.reportarFalla(this, justificacion);
  }

  public void marcarVencida() {
    this.estado.marcarVencida(this);
  }

  public void retornarAlDeposito() {
    this.estado.retornarAlDeposito(this);
  }
}
