package ar.edu.utn.donatrack.beneficiarios;

import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.MedioDeContacto;
import ar.edu.utn.donatrack.notificaciones.Envio;
import ar.edu.utn.donatrack.notificaciones.Notificable;
import ar.edu.utn.donatrack.notificaciones.NotificadorPorCanal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

@Entity
public class EntidadBeneficiaria implements Notificable {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String razonSocial;
  private String direccion;
  private String telefono;

  // Cada colección tiene su propia clave foránea: compartir entidad_id hacía que la colección
  // recurrente leyera filas Necesidad y Hibernate levantara WrongClassException, porque
  // NecesidadRecurrente hereda de Necesidad en una sola tabla. Con dos columnas, la pertenencia a
  // una colección es explícita y el tipo real de cada fila se conserva.
  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "entidad_id")
  private List<Necesidad> necesidadesExtraordinarias = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "entidad_recurrente_id")
  private List<NecesidadRecurrente> necesidadesRecurrentes = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "entidad_id")
  private List<CorreoElectronico> correos = new ArrayList<>();

  protected EntidadBeneficiaria() {}

  /**
   * Crea una entidad beneficiaria sin identificador.
   *
   * <p>El identificador lo asigna el proveedor de persistencia al guardar la entidad; una entidad
   * recién construida todavía no lo tiene.
   *
   * @param razonSocial razón social de la entidad
   * @param direccion dirección de la entidad
   * @param telefono teléfono de contacto de la entidad
   */
  public EntidadBeneficiaria(String razonSocial, String direccion, String telefono) {
    this.razonSocial = razonSocial;
    this.direccion = direccion;
    this.telefono = telefono;
  }

  public EntidadBeneficiaria(Long id, String razonSocial, String direccion, String telefono) {
    this.id = Objects.requireNonNull(id, "id no puede ser null");
    this.razonSocial = razonSocial;
    this.direccion = direccion;
    this.telefono = telefono;
  }

  /**
   * Devuelve el identificador de la entidad beneficiaria.
   *
   * @return el identificador de la entidad beneficiaria
   */
  public Long getId() {
    return id;
  }

  public String getRazonSocial() {
    return razonSocial;
  }

  public String getDireccion() {
    return direccion;
  }

  public String getTelefono() {
    return telefono;
  }

  public List<Necesidad> getNecesidadesExtraordinarias() {
    return Collections.unmodifiableList(necesidadesExtraordinarias);
  }

  public List<NecesidadRecurrente> getNecesidadesRecurrentes() {
    return Collections.unmodifiableList(necesidadesRecurrentes);
  }

  public List<CorreoElectronico> getCorreos() {
    return Collections.unmodifiableList(correos);
  }

  /** La entidad se notifica a todos sus correos. */
  @Override
  public List<Envio> notificar(String mensaje, NotificadorPorCanal canales) {
    return correos.stream().map(correo -> correo.enviar(mensaje, canales)).toList();
  }

  public List<MedioDeContacto> getMediosDeContacto() {
    return Collections.unmodifiableList(new ArrayList<>(correos));
  }

  public void agregarNecesidadExtraordinaria(Necesidad n) {
    this.necesidadesExtraordinarias.add(n);
  }

  public void agregarNecesidadRecurrente(NecesidadRecurrente n) {
    this.necesidadesRecurrentes.add(n);
  }

  public Optional<Necesidad> buscarNecesidad(UUID necesidadId) {
    return todasLasNecesidades().filter(n -> n.getId().equals(necesidadId)).findFirst();
  }

  public List<Necesidad> necesidades() {
    List<Necesidad> todas = new ArrayList<>(necesidadesExtraordinarias);
    todas.addAll(necesidadesRecurrentes);
    return Collections.unmodifiableList(todas);
  }

  public void eliminarNecesidad(UUID necesidadId) {
    necesidadesExtraordinarias.removeIf(n -> n.getId().equals(necesidadId));
    necesidadesRecurrentes.removeIf(n -> n.getId().equals(necesidadId));
  }

  public void agregarCorreo(CorreoElectronico correo) {
    this.correos.add(correo);
  }

  public void actualizarDatos(
      String razonSocial, String direccion, String telefono, List<CorreoElectronico> correos) {
    this.razonSocial = razonSocial;
    this.direccion = direccion;
    this.telefono = telefono;
    this.correos.clear();
    if (correos != null) {
      correos.forEach(this::agregarCorreo);
    }
  }

  /**
   * Indica si la entidad tiene alguna necesidad compatible con la donación.
   *
   * @param donacion donación que se evalúa (no nula)
   * @return {@code true} si alguna necesidad de la entidad es compatible con la donación
   * @throws NullPointerException si {@code donacion} es {@code null}
   */
  public boolean tieneNecesidadCompatible(Donacion donacion) {
    Objects.requireNonNull(donacion, "donacion no puede ser null");
    return todasLasNecesidades().anyMatch(donacion::esCompatibleCon);
  }

  /**
   * Calcula el mejor puntaje de compatibilidad entre sus necesidades y la donación.
   *
   * @param donacion donación que se puntúa (no nula)
   * @return el puntaje máximo entre sus necesidades, o {@code 0} si ninguna es compatible
   * @throws NullPointerException si {@code donacion} es {@code null}
   */
  public int calcularPuntajeCompatibilidad(Donacion donacion) {
    Objects.requireNonNull(donacion, "donacion no puede ser null");
    return todasLasNecesidades().mapToInt(donacion::calcularPuntajeCompatibilidad).max().orElse(0);
  }

  /**
   * Registra la donación en cada necesidad compatible de la entidad.
   *
   * @param donacion donación recibida (no nula)
   * @throws NullPointerException si {@code donacion} es {@code null}
   */
  public void aplicarDonacionRecibida(Donacion donacion) {
    Objects.requireNonNull(donacion, "donacion no puede ser null");
    todasLasNecesidades().forEach(donacion::aplicarA);
  }

  private Stream<Necesidad> todasLasNecesidades() {
    return Stream.concat(necesidadesExtraordinarias.stream(), necesidadesRecurrentes.stream());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EntidadBeneficiaria that)) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
