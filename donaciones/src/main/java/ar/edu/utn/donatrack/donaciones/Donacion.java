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
import java.util.UUID;

public class Donacion {
  private final UUID id;
  private EstadoDonacion estado = new EnDeposito();
  private final List<HistorialEstado> historialEstado = new ArrayList<>();
  private final List<Bien> bienes = new ArrayList<>();
  private final List<String> fotos = new ArrayList<>();
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
    this(UUID.randomUUID(), donante, bienes);
  }

  public Donacion(UUID id, Donante donante, List<Bien> bienes) {
    this.id = Objects.requireNonNull(id, "id no puede ser null");
    this.donante = Objects.requireNonNull(donante, "donante no puede ser null");
    reemplazarBienes(bienes);
  }

  public UUID getId() {
    return id;
  }

  public synchronized void cambiarEstado(
      HistorialEstado nuevoHistorial, EstadoDonacion nuevoEstado) {
    Objects.requireNonNull(nuevoHistorial, "historial no puede ser null");
    Objects.requireNonNull(nuevoEstado, "estado no puede ser null");
    historialEstado.add(
        nuevoHistorial.contextualizar(historialEstado.size() + 1L, estado.getNombreEstado()));
    estado = nuevoEstado;
  }

  public void reemplazarBienes(List<Bien> nuevosBienes) {
    if (nuevosBienes == null || nuevosBienes.isEmpty() || nuevosBienes.stream().anyMatch(Objects::isNull)) {
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

  public NombreEstado getNombreEstado() {
    return estado.getNombreEstado();
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
