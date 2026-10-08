package ar.edu.utn.donatrack.beneficiarios;

import ar.edu.utn.donatrack.donaciones.Donacion;
import ar.edu.utn.donatrack.donantes.CorreoElectronico;
import ar.edu.utn.donatrack.donantes.MedioDeContacto;
import ar.edu.utn.donatrack.notificaciones.Notificable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

@Entity
public class EntidadBeneficiaria implements Notificable {
  @Id private Long id;

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
   * Crea una entidad beneficiaria con un identificador nuevo.
   *
   * <p>El alta sin identificador explícito resuelve la asignación en el borde del servicio, igual
   * que {@code UUID.randomUUID()} resolvía el alta sin id en el modelo anterior.
   *
   * @param razonSocial razón social de la entidad
   * @param direccion dirección de la entidad
   * @param telefono teléfono de contacto de la entidad
   */
  public EntidadBeneficiaria(String razonSocial, String direccion, String telefono) {
    this(ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE), razonSocial, direccion, telefono);
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

  @Override
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
    return necesidades().stream().filter(n -> n.getId().equals(necesidadId)).findFirst();
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

  public void aplicarDonacionRecibida(Donacion donacion) {
    Objects.requireNonNull(donacion, "donacion no puede ser null");
    necesidadesExtraordinarias.forEach(necesidad -> aplicarSiCoincide(necesidad, donacion));
    necesidadesRecurrentes.forEach(necesidad -> aplicarSiCoincide(necesidad, donacion));
  }

  private void aplicarSiCoincide(Necesidad necesidad, Donacion donacion) {
    if (necesidad.getSubcategoria().equals(donacion.getSubcategoria())) {
      necesidad.recibirDonacion(donacion.getCantidadTotal());
    }
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
